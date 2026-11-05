package br.com.smartprod.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.smartprod.auth.TokenService.TokenEmitido;
import br.com.smartprod.usuario.Perfil;
import br.com.smartprod.usuario.Usuario;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    // Com milissegundos de propósito: o token só guarda segundos.
    private static final Instant AGORA = Instant.parse("2026-11-04T12:00:00.789Z");

    private final JwtProperties propriedades = new JwtProperties(SEGREDO, 60);
    private final TokenService service = new TokenService(new JwtConfig().jwtEncoder(propriedades), propriedades,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void emiteJwtAssinadoComPerfilEValidade() {
        TokenEmitido emitido = service.emitir(usuario(7L, Perfil.GERENTE));

        Jwt jwt = decodificar(emitido.valor(), SEGREDO);
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("smartprod");
        assertThat(jwt.getClaimAsString("perfil")).isEqualTo("GERENTE");
        assertThat(jwt.getIssuedAt()).isEqualTo(Instant.parse("2026-11-04T12:00:00Z"));
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-11-04T13:00:00Z"));
        assertThat(emitido.expiraEm()).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void tokenDeFuncionarioLevaOPerfilFuncionario() {
        TokenEmitido emitido = service.emitir(usuario(8L, Perfil.FUNCIONARIO));

        assertThat(decodificar(emitido.valor(), SEGREDO).getClaimAsString("perfil")).isEqualTo("FUNCIONARIO");
    }

    @Test
    void naoLevaNomeEmailNemSenhaNoToken() { // o JWT é só codificado, qualquer um consegue ler
        TokenEmitido emitido = service.emitir(usuario(7L, Perfil.GERENTE));

        assertThat(decodificar(emitido.valor(), SEGREDO).getClaims())
                .containsOnlyKeys("sub", "iss", "iat", "exp", "perfil");
    }

    @Test
    void tokenNaoValidaComOutroSegredo() {
        TokenEmitido emitido = service.emitir(usuario(7L, Perfil.GERENTE));

        assertThatThrownBy(() -> decodificar(emitido.valor(), "outro-segredo-diferente-com-32-caracteres"))
                .isInstanceOf(BadJwtException.class);
    }

    private static Usuario usuario(Long id, Perfil perfil) {
        Usuario usuario = new Usuario("Maria", "maria@padaria.com", "hash", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    /** Decodifica conferindo só a assinatura: o relógio do emissor no teste é fixo, então o prazo não vale. */
    private static Jwt decodificar(String token, String segredo) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(JwtConfig.chave(new JwtProperties(segredo, 1)))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder.decode(token);
    }
}
