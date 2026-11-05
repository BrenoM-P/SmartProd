package br.com.smartprod.insumo.dto;

import br.com.smartprod.insumo.Insumo;
import br.com.smartprod.insumo.UnidadeMedida;

/**
 * Insumo como a API devolve. Existem duas formas e a escolha é pelo perfil de quem consulta:
 * o Gerente recebe {@link InsumoGerenteResponse} (com custo) e o Funcionário recebe
 * {@link InsumoFuncionarioResponse}, que <b>não tem o campo de custo</b> (RNF-S002).
 */
public sealed interface InsumoResponse permits InsumoGerenteResponse, InsumoFuncionarioResponse {

    Long id();

    String nome();

    UnidadeMedida unidadeMedida();

    static InsumoResponse de(Insumo insumo, boolean gerente) {
        return gerente ? InsumoGerenteResponse.de(insumo) : InsumoFuncionarioResponse.de(insumo);
    }
}
