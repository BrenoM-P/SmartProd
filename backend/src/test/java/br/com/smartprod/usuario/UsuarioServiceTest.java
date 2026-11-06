package br.com.smartprod.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import br.com.smartprod.usuario.dto.UsuarioCriacaoRequest;
import br.com.smartprod.usuario.dto.UsuarioEdicaoRequest;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // força baixa só para o teste ser rápido
    private UsuarioService service;

    @BeforeEach
    void criarServico() {
        service = new UsuarioService(repository, encoder);
    }

    // ---- criar

    @Test
    void criaComEmailNormalizadoNomeSemEspacosESenhaEmBcrypt() {
        when(repository.existsByEmail("ana@padaria.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Usuario criado = service.criar(
                new UsuarioCriacaoRequest("  Ana Souza ", " Ana@Padaria.com ", "senha-forte-1", Perfil.FUNCIONARIO));

        assertThat(criado.getNome()).isEqualTo("Ana Souza");
        assertThat(criado.getEmail()).isEqualTo("ana@padaria.com");
        assertThat(criado.getPerfil()).isEqualTo(Perfil.FUNCIONARIO);
        assertThat(criado.isAtivo()).isTrue();
        assertThat(criado.getSenhaHash()).isNotEqualTo("senha-forte-1").startsWith("$2a$");
        assertThat(encoder.matches("senha-forte-1", criado.getSenhaHash())).isTrue();
    }

    @Test
    void emailJaCadastradoLancaConflito() {
        when(repository.existsByEmail("ana@padaria.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(
                new UsuarioCriacaoRequest("Ana", "ANA@padaria.com", "senha-forte-1", Perfil.FUNCIONARIO)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void cadastroSimultaneoBarradoPeloBancoLancaConflito() {
        when(repository.existsByEmail("ana@padaria.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("uk_usuarios_email"));

        assertThatThrownBy(() -> service.criar(
                new UsuarioCriacaoRequest("Ana", "ana@padaria.com", "senha-forte-1", Perfil.FUNCIONARIO)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void senhaAcimaDe72BytesLancaErroDeValidacao() {
        when(repository.existsByEmail("ana@padaria.com")).thenReturn(false);

        // 40 caracteres passam no @Size, mas cada "ç" ocupa 2 bytes: são 80 bytes.
        assertThatThrownBy(() -> service.criar(
                new UsuarioCriacaoRequest("Ana", "ana@padaria.com", "ç".repeat(40), Perfil.FUNCIONARIO)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void senhaDeExatos72BytesEAceita() {
        when(repository.existsByEmail("ana@padaria.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Usuario criado = service.criar(
                new UsuarioCriacaoRequest("Ana", "ana@padaria.com", "ç".repeat(36), Perfil.FUNCIONARIO));

        assertThat(encoder.matches("ç".repeat(36), criado.getSenhaHash())).isTrue();
    }

    // ---- consultar

    @Test
    void listaTodosQuandoNaoHaFiltroEFiltraPorAtivo() {
        List<Usuario> todos = List.of(usuario(1L, "a@padaria.com", Perfil.GERENTE));
        List<Usuario> inativos = List.of(usuario(2L, "b@padaria.com", Perfil.FUNCIONARIO));
        when(repository.findAllByOrderByNomeAsc()).thenReturn(todos);
        when(repository.findByAtivoOrderByNomeAsc(false)).thenReturn(inativos);

        assertThat(service.listar(null)).isSameAs(todos);
        assertThat(service.listar(false)).isSameAs(inativos);
    }

    @Test
    void usuarioInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---- atualizar

    @Test
    void atualizaDadosENaoMexeNaSenhaQuandoNaoInformada() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("novo@padaria.com", 5L)).thenReturn(false);

        service.atualizar(5L, new UsuarioEdicaoRequest(" Beto Novo ", " Novo@Padaria.com ", Perfil.GERENTE, null), 1L);

        assertThat(existente.getNome()).isEqualTo("Beto Novo");
        assertThat(existente.getEmail()).isEqualTo("novo@padaria.com");
        assertThat(existente.getPerfil()).isEqualTo(Perfil.GERENTE);
        assertThat(existente.getSenhaHash()).isEqualTo("hash-antigo");
    }

    @Test
    void trocaASenhaComBcryptQuandoInformada() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        service.atualizar(5L, new UsuarioEdicaoRequest("Beto", "beto@padaria.com", Perfil.FUNCIONARIO, "nova-senha-1"),
                1L);

        assertThat(existente.getSenhaHash()).isNotEqualTo("hash-antigo");
        assertThat(encoder.matches("nova-senha-1", existente.getSenhaHash())).isTrue();
    }

    @Test
    void emailDeOutroUsuarioLancaConflito() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("ana@padaria.com", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(5L,
                new UsuarioEdicaoRequest("Beto", "ana@padaria.com", Perfil.FUNCIONARIO, null), 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(existente.getEmail()).isEqualTo("beto@padaria.com");
    }

    @Test
    void edicaoSimultaneaBarradaPeloBancoLancaConflito() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("ana@padaria.com", 5L)).thenReturn(false);
        when(repository.saveAndFlush(existente)).thenThrow(new DataIntegrityViolationException("uk_usuarios_email"));

        assertThatThrownBy(() -> service.atualizar(5L,
                new UsuarioEdicaoRequest("Beto", "ana@padaria.com", Perfil.FUNCIONARIO, null), 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    /** O login recusa senha em branco: aceitá-la aqui deixaria o usuário sem conseguir entrar. */
    @Test
    void senhaSoDeEspacosNaEdicaoLancaErroDeValidacaoEMantemASenha() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(5L,
                new UsuarioEdicaoRequest("Beto", "beto@padaria.com", Perfil.FUNCIONARIO, "        "), 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(existente.getSenhaHash()).isEqualTo("hash-antigo");
    }

    @Test
    void senhaAcimaDe72BytesNaEdicaoLancaErroDeValidacaoEMantemASenha() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(5L,
                new UsuarioEdicaoRequest("Beto", "beto@padaria.com", Perfil.FUNCIONARIO, "ç".repeat(40)), 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(existente.getSenhaHash()).isEqualTo("hash-antigo");
    }

    @Test
    void mantendoOMesmoEmailNaoConsultaDuplicidade() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        service.atualizar(5L, new UsuarioEdicaoRequest("Beto", " BETO@padaria.com", Perfil.FUNCIONARIO, null), 1L);

        verify(repository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void naoPermiteMudarOProprioPerfil() {
        Usuario chefe = usuario(1L, "chefe@padaria.com", Perfil.GERENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(chefe));

        assertThatThrownBy(() -> service.atualizar(1L,
                new UsuarioEdicaoRequest("Chefe", "chefe@padaria.com", Perfil.FUNCIONARIO, null), 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(chefe.getPerfil()).isEqualTo(Perfil.GERENTE);
    }

    @Test
    void mantendoOProprioPerfilPodeEditarOProprioCadastro() {
        Usuario chefe = usuario(1L, "chefe@padaria.com", Perfil.GERENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(chefe));

        service.atualizar(1L, new UsuarioEdicaoRequest("Chefe Novo", "chefe@padaria.com", Perfil.GERENTE, null), 1L);

        assertThat(chefe.getNome()).isEqualTo("Chefe Novo");
    }

    // ---- inativar

    @Test
    void inativaEMantemOUsuarioSemRemover() {
        Usuario existente = usuario(5L, "beto@padaria.com", Perfil.FUNCIONARIO);
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        Usuario inativado = service.inativar(5L, 1L);

        assertThat(inativado.isAtivo()).isFalse();
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void naoPermiteInativarAPropriaConta() {
        Usuario chefe = usuario(1L, "chefe@padaria.com", Perfil.GERENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(chefe));

        assertThatThrownBy(() -> service.inativar(1L, 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(chefe.isAtivo()).isTrue();
    }

    @Test
    void inativarUsuarioInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inativar(99L, 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static Usuario usuario(Long id, String email, Perfil perfil) {
        Usuario usuario = new Usuario("Teste", email, "hash-antigo", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
