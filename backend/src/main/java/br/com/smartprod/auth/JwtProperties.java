package br.com.smartprod.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Propriedades do token de acesso (prefixo {@code app.jwt}).
 *
 * <p>O segredo vem da variável de ambiente {@code JWT_SECRET} (ver {@code application-*.yml});
 * nunca fica no repositório, exceto o valor de desenvolvimento do perfil dev.
 *
 * @param secret          segredo HS256, com no mínimo 32 caracteres
 * @param validadeMinutos tempo de vida do token, em minutos
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, @DefaultValue("480") long validadeMinutos) {

    /** Impede que o segredo apareça em log se o objeto for impresso por engano. */
    @Override
    public String toString() {
        return "JwtProperties[secret=***, validadeMinutos=" + validadeMinutos + "]";
    }
}
