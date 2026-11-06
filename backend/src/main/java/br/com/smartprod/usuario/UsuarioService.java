package br.com.smartprod.usuario;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.smartprod.usuario.dto.UsuarioCriacaoRequest;
import br.com.smartprod.usuario.dto.UsuarioEdicaoRequest;

/**
 * Regras do cadastro de usuários (RF002): e-mail único, senha só em BCrypt, usuários inativados e
 * nunca removidos, e proteção para o Gerente não se trancar para fora do sistema.
 *
 * <p>Os erros saem como {@link ResponseStatusException}, que o Spring devolve em RFC 7807.
 */
@Service
public class UsuarioService {

    /** Limite do BCrypt, em bytes: acima disso ele recusa a senha. */
    private static final int MAXIMO_DE_BYTES_DA_SENHA = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario criar(UsuarioCriacaoRequest requisicao) {
        String email = Usuario.normalizarEmail(requisicao.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw emailEmUso();
        }
        return salvar(new Usuario(requisicao.nome().trim(), email, codificarSenha(requisicao.senha()),
                requisicao.perfil()));
    }

    /** @param ativo {@code true} só ativos, {@code false} só inativos, {@code null} todos. */
    @Transactional(readOnly = true)
    public List<Usuario> listar(Boolean ativo) {
        return ativo == null
                ? usuarioRepository.findAllByOrderByNomeAsc()
                : usuarioRepository.findByAtivoOrderByNomeAsc(ativo);
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    /**
     * @param solicitanteId id de quem está editando (ou {@code null}); o Gerente não pode mudar o
     *                      próprio perfil, para nunca sobrar o sistema sem Gerente.
     */
    @Transactional
    public Usuario atualizar(Long id, UsuarioEdicaoRequest requisicao, Long solicitanteId) {
        Usuario usuario = buscar(id);
        String email = Usuario.normalizarEmail(requisicao.email());

        if (!email.equals(usuario.getEmail()) && usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw emailEmUso();
        }
        if (id.equals(solicitanteId) && requisicao.perfil() != usuario.getPerfil()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode alterar o próprio perfil.");
        }

        usuario.atualizar(requisicao.nome().trim(), email, requisicao.perfil());
        if (requisicao.senha() != null) {
            usuario.alterarSenhaHash(codificarSenha(requisicao.senha()));
        }
        salvar(usuario);
        return usuario;
    }

    /** RF002: inativa e mantém o registro. Quem inativa não pode ser o próprio usuário. */
    @Transactional
    public Usuario inativar(Long id, Long solicitanteId) {
        Usuario usuario = buscar(id);
        if (id.equals(solicitanteId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode inativar a própria conta.");
        }
        usuario.inativar();
        usuarioRepository.save(usuario);
        return usuario;
    }

    /**
     * Grava já, sem esperar o fim da transação, para o erro poder ser tratado aqui. A conferência prévia
     * do e-mail não cobre dois envios simultâneos (clique duplo): quem barra o segundo é a restrição única
     * do banco, e a resposta precisa continuar sendo 409.
     */
    private Usuario salvar(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw emailEmUso();
        }
    }

    /**
     * Regras que os DTOs não cobrem: só espaços é recusado (o login também recusa, e o usuário ficaria sem
     * entrar) e o limite do BCrypt é em bytes, não em caracteres (um "ç" ocupa 2 bytes em UTF-8).
     */
    private String codificarSenha(String senha) {
        if (senha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha não pode ser só de espaços.");
        }
        if (senha.getBytes(UTF_8).length > MAXIMO_DE_BYTES_DA_SENHA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha é longa demais. Use no máximo 72 caracteres, ou menos se houver letras com acento.");
        }
        return passwordEncoder.encode(senha);
    }

    private static ResponseStatusException emailEmUso() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este e-mail.");
    }
}
