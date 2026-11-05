package br.com.smartprod.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.smartprod.usuario.Perfil;
import br.com.smartprod.usuario.Usuario;
import br.com.smartprod.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioJwtConverterTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private UsuarioJwtConverter conversor;

    @Test
    void usuarioAtivoViraAutenticacaoComRoleDoPerfilEIdComoNome() {
        when(repository.findById(7L)).thenReturn(Optional.of(usuario(7L, Perfil.GERENTE, true)));

        AbstractAuthenticationToken autenticacao = conversor.convert(jwt("7", "GERENTE"));

        assertThat(autenticacao.getName()).isEqualTo("7");
        assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_GERENTE");
    }

    @Test
    void perfilVemDoBancoENaoDoClaimDoToken() {
        when(repository.findById(8L)).thenReturn(Optional.of(usuario(8L, Perfil.FUNCIONARIO, true)));

        AbstractAuthenticationToken autenticacao = conversor.convert(jwt("8", "GERENTE"));

        assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_FUNCIONARIO");
    }

    @Test
    void usuarioInativoELevantaTokenInvalido() {
        when(repository.findById(9L)).thenReturn(Optional.of(usuario(9L, Perfil.FUNCIONARIO, false)));

        assertThatThrownBy(() -> conversor.convert(jwt("9", "FUNCIONARIO")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void usuarioInexistenteLevantaTokenInvalido() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversor.convert(jwt("10", "GERENTE")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void subjectQueNaoENumeroLevantaTokenInvalido() {
        assertThatThrownBy(() -> conversor.convert(jwt("abc", "GERENTE")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void tokenSemSubjectLevantaTokenInvalido() {
        Jwt semSubject = Jwt.withTokenValue("t").header("alg", "HS256").claim("perfil", "GERENTE").build();

        assertThatThrownBy(() -> conversor.convert(semSubject)).isInstanceOf(InvalidBearerTokenException.class);
    }

    private static Jwt jwt(String subject, String perfil) {
        return Jwt.withTokenValue("t").header("alg", "HS256").subject(subject).claim("perfil", perfil).build();
    }

    private static Usuario usuario(Long id, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario("Teste", "teste@padaria.com", "hash", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        if (!ativo) {
            usuario.inativar();
        }
        return usuario;
    }
}
