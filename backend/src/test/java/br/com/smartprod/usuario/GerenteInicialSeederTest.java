package br.com.smartprod.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class GerenteInicialSeederTest {

    @Mock
    private UsuarioRepository repository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // força baixa só para o teste ser rápido

    @Test
    void criaGerenteComSenhaEmBcryptQuandoOEmailNaoExiste() {
        when(repository.findByEmail("gerente@padaria.com")).thenReturn(Optional.empty());

        seeder("Chefe", " Gerente@Padaria.com ", "senha-forte-123").run(null);

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(capturado.capture());
        Usuario criado = capturado.getValue();
        assertThat(criado.getNome()).isEqualTo("Chefe");
        assertThat(criado.getEmail()).isEqualTo("gerente@padaria.com");
        assertThat(criado.getPerfil()).isEqualTo(Perfil.GERENTE);
        assertThat(criado.isAtivo()).isTrue();
        assertThat(criado.getSenhaHash()).isNotEqualTo("senha-forte-123").startsWith("$2a$");
        assertThat(encoder.matches("senha-forte-123", criado.getSenhaHash())).isTrue();
    }

    @Test
    void naoRecriaNemSobrescreveQuandoOEmailJaExiste() {
        when(repository.findByEmail("gerente@padaria.com")).thenReturn(
                Optional.of(new Usuario("Chefe", "gerente@padaria.com", "hash-antigo", Perfil.GERENTE)));

        seeder("Chefe", "gerente@padaria.com", "senha-forte-123").run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void naoFazNadaSemEmailOuSemSenha() {
        seeder("Gerente", null, null).run(null);
        seeder("Gerente", "", "senha-forte-123").run(null);
        seeder("Gerente", "gerente@padaria.com", "  ").run(null);

        verifyNoInteractions(repository);
    }

    @Test
    void propriedadesNaoExibemEmailNemSenhaNoToString() {
        assertThat(new SeedGerenteProperties("Chefe", "gerente@padaria.com", "senha-forte-123").toString())
                .doesNotContain("gerente@padaria.com")
                .doesNotContain("senha-forte-123");
    }

    private GerenteInicialSeeder seeder(String nome, String email, String senha) {
        return new GerenteInicialSeeder(repository, encoder, new SeedGerenteProperties(nome, email, senha));
    }
}
