package br.com.smartprod.insumo;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Insumo (matéria-prima) com o custo por unidade de medida (RF004). Nunca sai direto na API: use os DTOs. */
@Entity
@Table(name = "insumos")
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 10)
    private UnidadeMedida unidadeMedida;

    /** Custo de uma unidade de medida, em R$ (até 4 casas: o custo por grama, por exemplo, é muito pequeno). */
    @Column(name = "custo_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal custoUnitario;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    protected Insumo() {
    }

    public Insumo(String nome, UnidadeMedida unidadeMedida, BigDecimal custoUnitario) {
        this.nome = nome;
        this.unidadeMedida = unidadeMedida;
        this.custoUnitario = custoUnitario;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public UnidadeMedida getUnidadeMedida() {
        return unidadeMedida;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void atualizar(String nome, UnidadeMedida unidadeMedida, BigDecimal custoUnitario) {
        this.nome = nome;
        this.unidadeMedida = unidadeMedida;
        this.custoUnitario = custoUnitario;
    }

    /** Insumo inativado some das listas, mas continua cadastrado (fichas técnicas podem referenciá-lo). */
    public void inativar() {
        this.ativo = false;
    }
}
