package br.com.smartprod.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** Configuração do JWT (RF001): assinatura e validação HS256 com o segredo de {@code app.jwt.secret}. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    /** HS256 exige chave de no mínimo 256 bits (32 bytes). */
    static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    @Bean
    JwtEncoder jwtEncoder(JwtProperties propriedades) {
        if (propriedades.validadeMinutos() <= 0) {
            throw new IllegalStateException("app.jwt.validade-minutos (JWT_VALIDADE_MINUTOS) deve ser maior que zero.");
        }
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    /** Lê o token Bearer: confere a assinatura, o prazo (exp/nbf) e o emissor ({@code smartprod}). */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(propriedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.EMISSOR));
        return decoder;
    }

    /** O claim {@code perfil} do token vira a autoridade {@code ROLE_GERENTE} ou {@code ROLE_FUNCIONARIO}. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(TokenService.CLAIM_PERFIL);
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    /** Chave de assinatura. Falha na subida da aplicação se o segredo estiver ausente ou fraco demais. */
    static SecretKey chave(JwtProperties propriedades) {
        String segredo = propriedades.secret();
        if (segredo == null || segredo.getBytes(UTF_8).length < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) deve ter no mínimo " + TAMANHO_MINIMO_DO_SEGREDO + " caracteres.");
        }
        return new SecretKeySpec(segredo.getBytes(UTF_8), "HmacSHA256");
    }
}
