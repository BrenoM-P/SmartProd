package br.com.smartprod.insumo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.smartprod.insumo.dto.InsumoRequest;

/**
 * Regras do cadastro de insumos (RF004). Insumo é inativado, nunca removido, porque as fichas técnicas
 * (dupla 2) vão referenciá-lo. Os erros saem como {@link ResponseStatusException} (RFC 7807).
 */
@Service
public class InsumoService {

    private final InsumoRepository insumoRepository;

    public InsumoService(InsumoRepository insumoRepository) {
        this.insumoRepository = insumoRepository;
    }

    @Transactional
    public Insumo criar(InsumoRequest requisicao) {
        return insumoRepository.save(
                new Insumo(requisicao.nome().trim(), requisicao.unidadeMedida(), requisicao.custoUnitario()));
    }

    /** @param incluirInativos {@code false} devolve só os ativos, os que podem entrar numa ficha técnica. */
    @Transactional(readOnly = true)
    public List<Insumo> listar(boolean incluirInativos) {
        return incluirInativos
                ? insumoRepository.findAllByOrderByNomeAsc()
                : insumoRepository.findByAtivoTrueOrderByNomeAsc();
    }

    /** Consulta pelo id mesmo se o insumo estiver inativo: uma ficha antiga ainda pode referenciá-lo. */
    @Transactional(readOnly = true)
    public Insumo buscar(Long id) {
        return insumoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Insumo não encontrado."));
    }

    @Transactional
    public Insumo atualizar(Long id, InsumoRequest requisicao) {
        Insumo insumo = buscar(id);
        insumo.atualizar(requisicao.nome().trim(), requisicao.unidadeMedida(), requisicao.custoUnitario());
        insumoRepository.save(insumo);
        return insumo;
    }

    @Transactional
    public Insumo inativar(Long id) {
        Insumo insumo = buscar(id);
        insumo.inativar();
        insumoRepository.save(insumo);
        return insumo;
    }
}
