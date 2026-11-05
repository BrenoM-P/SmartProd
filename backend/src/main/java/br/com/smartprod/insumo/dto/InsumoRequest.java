package br.com.smartprod.insumo.dto;

import java.math.BigDecimal;

import br.com.smartprod.insumo.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/insumos} e {@code PUT /api/insumos/{id}} (RF004). O custo tem até 4 casas. */
public record InsumoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotNull UnidadeMedida unidadeMedida,
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 4) BigDecimal custoUnitario) {
}
