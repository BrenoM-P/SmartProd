package br.com.smartprod.insumo.dto;

import br.com.smartprod.insumo.Insumo;
import br.com.smartprod.insumo.UnidadeMedida;

/** Insumo para o Funcionário: <b>sem custo</b> (RNF-S002). */
public record InsumoFuncionarioResponse(Long id, String nome, UnidadeMedida unidadeMedida)
        implements InsumoResponse {

    public static InsumoFuncionarioResponse de(Insumo insumo) {
        return new InsumoFuncionarioResponse(insumo.getId(), insumo.getNome(), insumo.getUnidadeMedida());
    }
}
