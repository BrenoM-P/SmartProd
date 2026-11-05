package br.com.smartprod.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.smartprod.usuario.Usuario;

/**
 * Emite o token JWT de acesso (RF001).
 *
 * <p>Claims: {@code sub} = id do usuário, {@code perfil} = GERENTE ou FUNCIONARIO, mais emissor e
 * validade. Não leva nome nem e-mail: o JWT é apenas codificado, não criptografado.
 */
@Service
public class TokenService {

    static final String EMISSOR = "smartprod";
    static final String CLAIM_PERFIL = "perfil";

    private final JwtEncoder encoder;
    private final JwtProperties propriedades;
    private final Clock relogio;

    @Autowired
    public TokenService(JwtEncoder encoder, JwtProperties propriedades) {
        this(encoder, propriedades, Clock.systemUTC());
    }

    TokenService(JwtEncoder encoder, JwtProperties propriedades, Clock relogio) {
        this.encoder = encoder;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    public TokenEmitido emitir(Usuario usuario) {
        // O JWT guarda datas em segundos; truncar aqui faz a resposta da API bater com o "exp" do token.
        Instant emitidoEm = relogio.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = emitidoEm.plus(Duration.ofMinutes(propriedades.validadeMinutos()));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(valor, expiraEm);
    }

    /** Token assinado e o instante em que ele expira. */
    public record TokenEmitido(String valor, Instant expiraEm) {
    }
}
