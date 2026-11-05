package br.com.smartprod.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import br.com.smartprod.usuario.Usuario;
import br.com.smartprod.usuario.UsuarioRepository;

/**
 * Transforma um token já validado (assinatura, prazo, emissor) na autenticação da requisição.
 *
 * <p>O token só diz <em>quem</em> é (claim {@code sub} = id). O usuário e o perfil são lidos do banco a
 * cada requisição, então inativar um usuário ou mudar o perfil dele vale na hora, sem esperar o token
 * expirar. Usuário inexistente ou inativo resulta em 401. O nome da autenticação é o id do usuário.
 */
@Component
public class UsuarioJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    public UsuarioJwtConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = idDoToken(jwt)
                .flatMap(usuarioRepository::findById)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new InvalidBearerTokenException("Usuário inexistente ou inativo."));

        List<SimpleGrantedAuthority> autoridades =
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
        return new JwtAuthenticationToken(jwt, autoridades, String.valueOf(usuario.getId()));
    }

    private static Optional<Long> idDoToken(Jwt jwt) {
        try {
            return Optional.of(Long.valueOf(jwt.getSubject()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
