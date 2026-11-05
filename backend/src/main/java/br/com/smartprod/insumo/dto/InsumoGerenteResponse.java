package br.com.smartprod.insumo.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.smartprod.insumo.Insumo;
import br.com.smartprod.insumo.UnidadeMedida;

/** Insumo para o Gerente: inclui o custo unitário e a situação (ativo). */
public record InsumoGerenteResponse(Long id, String nome, UnidadeMedida unidadeMedida, BigDecimal custoUnitario,
        boolean ativo) implements InsumoResponse {

    public static InsumoGerenteResponse de(Insumo insumo) {
        return new InsumoGerenteResponse(insumo.getId(), insumo.getNome(), insumo.getUnidadeMedida(),
                formatarCusto(insumo.getCustoUnitario()), insumo.isAtivo());
    }

    /** Mostra de 2 a 4 casas (6.50, 0.0065), sem os zeros que o banco acrescenta (6.5000). */
    static BigDecimal formatarCusto(BigDecimal custo) {
        return custo.setScale(Math.max(2, custo.stripTrailingZeros().scale()), RoundingMode.UNNECESSARY);
    }
}
