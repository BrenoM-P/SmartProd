package br.com.smartprod.usuario;

import java.util.List;

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
        return usuarioRepository.save(new Usuario(requisicao.nome().trim(), email,
                passwordEncoder.encode(requisicao.senha()), requisicao.perfil()));
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
            usuario.alterarSenhaHash(passwordEncoder.encode(requisicao.senha()));
        }
        usuarioRepository.save(usuario);
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

    private static ResponseStatusException emailEmUso() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com este e-mail.");
    }
}
