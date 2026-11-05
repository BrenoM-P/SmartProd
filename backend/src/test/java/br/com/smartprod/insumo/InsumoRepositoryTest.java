package br.com.smartprod.insumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

// Usa o mesmo H2 (modo PostgreSQL) e as mesmas migrações Flyway do perfil dev.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InsumoRepositoryTest {

    @Autowired
    private InsumoRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void salvaEGuardaOCustoComQuatroCasasDecimais() {
        Insumo salvo = repository.saveAndFlush(new Insumo("Farinha de trigo", UnidadeMedida.G, new BigDecimal("0.0065")));
        entityManager.clear();

        Insumo lido = repository.findById(salvo.getId()).orElseThrow();

        assertThat(lido.getCustoUnitario()).isEqualByComparingTo("0.0065");
        assertThat(lido.getUnidadeMedida()).isEqualTo(UnidadeMedida.G);
        assertThat(lido.isAtivo()).isTrue();
        assertThat(lido.getCriadoEm()).isNotNull();
    }

    @Test
    void listaPorNomeEFiltraOsAtivos() {
        repository.saveAndFlush(new Insumo("Zimbro", UnidadeMedida.UN, new BigDecimal("1.00")));
        repository.saveAndFlush(new Insumo("Açúcar", UnidadeMedida.KG, new BigDecimal("5.00")));
        Insumo inativo = repository.saveAndFlush(new Insumo("Manteiga", UnidadeMedida.KG, new BigDecimal("40.00")));
        inativo.inativar();
        repository.saveAndFlush(inativo);

        assertThat(repository.findByAtivoTrueOrderByNomeAsc()).extracting(Insumo::getNome)
                .contains("Açúcar", "Zimbro")
                .doesNotContain("Manteiga")
                .isSorted();
        assertThat(repository.findAllByOrderByNomeAsc()).extracting(Insumo::getNome).contains("Manteiga");
    }

    @Test
    void bancoRecusaCustoNegativo() {
        assertThatThrownBy(() -> repository.saveAndFlush(new Insumo("Sal", UnidadeMedida.KG, new BigDecimal("-1"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existeIndiceNaColunaNome() { // RNF-D002
        Number indices = (Number) entityManager.getEntityManager().createNativeQuery(
                "select count(*) from information_schema.indexes "
                        + "where lower(table_name) = 'insumos' and lower(index_name) = 'idx_insumos_nome'")
                .getSingleResult();

        assertThat(indices.intValue()).isGreaterThan(0);
    }
}
