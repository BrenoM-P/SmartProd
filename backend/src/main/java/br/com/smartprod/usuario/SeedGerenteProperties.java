package br.com.smartprod.usuario;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Dados do primeiro Gerente (prefixo {@code app.seed.gerente}). O perfil dev traz valores de exemplo;
 * em produção vêm das variáveis {@code SEED_GERENTE_EMAIL} e {@code SEED_GERENTE_SENHA}.
 */
@ConfigurationProperties(prefix = "app.seed.gerente")
public record SeedGerenteProperties(@DefaultValue("Gerente") String nome, String email, String senha) {

    /** A seed só roda quando e-mail e senha foram informados. */
    boolean preenchido() {
        return email != null && !email.isBlank() && senha != null && !senha.isBlank();
    }

    /** Evita que a senha apareça em log se o objeto for impresso por engano. */
    @Override
    public String toString() {
        return "SeedGerenteProperties[nome=" + nome + ", email=***, senha=***]";
    }
}
