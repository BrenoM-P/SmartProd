package br.com.smartprod.insumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import br.com.smartprod.insumo.dto.InsumoRequest;

@ExtendWith(MockitoExtension.class)
class InsumoServiceTest {

    @Mock
    private InsumoRepository repository;

    @InjectMocks
    private InsumoService service;

    @Test
    void criaComNomeSemEspacosNasPontas() {
        when(repository.save(any(Insumo.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Insumo criado = service.criar(new InsumoRequest("  Farinha de trigo ", UnidadeMedida.KG, new BigDecimal("6.50")));

        assertThat(criado.getNome()).isEqualTo("Farinha de trigo");
        assertThat(criado.getUnidadeMedida()).isEqualTo(UnidadeMedida.KG);
        assertThat(criado.getCustoUnitario()).isEqualByComparingTo("6.50");
        assertThat(criado.isAtivo()).isTrue();
    }

    @Test
    void listaSoOsAtivosPorPadraoEOsInativosTambemQuandoPedido() {
        List<Insumo> ativos = List.of(new Insumo("A", UnidadeMedida.G, BigDecimal.ONE));
        List<Insumo> todos = List.of(new Insumo("A", UnidadeMedida.G, BigDecimal.ONE),
                new Insumo("B", UnidadeMedida.G, BigDecimal.ONE));
        when(repository.findByAtivoTrueOrderByNomeAsc()).thenReturn(ativos);
        when(repository.findAllByOrderByNomeAsc()).thenReturn(todos);

        assertThat(service.listar(false)).isSameAs(ativos);
        assertThat(service.listar(true)).isSameAs(todos);
    }

    @Test
    void buscaMesmoInsumoInativo() {
        Insumo inativo = new Insumo("Velho", UnidadeMedida.UN, BigDecimal.ONE);
        inativo.inativar();
        when(repository.findById(3L)).thenReturn(Optional.of(inativo));

        assertThat(service.buscar(3L)).isSameAs(inativo);
    }

    @Test
    void insumoInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        erro -> assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void atualizaNomeUnidadeECusto() {
        Insumo existente = new Insumo("Farinha", UnidadeMedida.KG, new BigDecimal("6.50"));
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        Insumo atualizado = service.atualizar(1L,
                new InsumoRequest(" Farinha integral ", UnidadeMedida.G, new BigDecimal("0.0080")));

        assertThat(atualizado.getNome()).isEqualTo("Farinha integral");
        assertThat(atualizado.getUnidadeMedida()).isEqualTo(UnidadeMedida.G);
        assertThat(atualizado.getCustoUnitario()).isEqualByComparingTo("0.0080");
        assertThat(atualizado.isAtivo()).isTrue();
    }

    @Test
    void atualizarInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, new InsumoRequest("X", UnidadeMedida.G, BigDecimal.ONE)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void inativaEMantemOInsumoSemRemover() {
        Insumo existente = new Insumo("Fermento", UnidadeMedida.G, new BigDecimal("0.05"));
        when(repository.findById(2L)).thenReturn(Optional.of(existente));

        Insumo inativado = service.inativar(2L);

        assertThat(inativado.isAtivo()).isFalse();
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void inativarInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inativar(99L)).isInstanceOf(ResponseStatusException.class);
    }
}
