package br.com.smartprod.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtConfigTest {

    private final JwtConfig config = new JwtConfig();

    @Test
    void segredoComMenosDe32CaracteresImpedeASubida() {
        assertThatThrownBy(() -> config.jwtEncoder(new JwtProperties("a".repeat(31), 60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void segredoAusenteImpedeASubida() {
        assertThatThrownBy(() -> config.jwtEncoder(new JwtProperties(null, 60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void segredoComExatamente32CaracteresEAceito() {
        assertThat(config.jwtEncoder(new JwtProperties("a".repeat(32), 60))).isNotNull();
    }

    @Test
    void validadeZeroOuNegativaImpedeASubida() {
        assertThatThrownBy(() -> config.jwtEncoder(new JwtProperties("a".repeat(32), 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_VALIDADE_MINUTOS");
    }

    @Test
    void propriedadesNaoExibemOSegredoNoToString() {
        assertThat(new JwtProperties("segredo-super-secreto-com-32-caracteres!", 60).toString())
                .doesNotContain("segredo-super-secreto");
    }
}
