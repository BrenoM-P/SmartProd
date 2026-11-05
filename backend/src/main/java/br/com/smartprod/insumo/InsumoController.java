package br.com.smartprod.insumo;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.smartprod.insumo.dto.InsumoGerenteResponse;
import br.com.smartprod.insumo.dto.InsumoRequest;
import br.com.smartprod.insumo.dto.InsumoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Cadastro de insumos (RF004). A leitura é de qualquer usuário logado, mas o Funcionário recebe um DTO
 * sem custo (RNF-S002); criar, editar e inativar são só do Gerente.
 */
@RestController
@RequestMapping("/api/insumos")
@Tag(name = "Insumos", description = "Cadastro de insumos (RF004). Funcionário só lê, e sem o custo unitário")
public class InsumoController {

    private static final String ROLE_GERENTE = "ROLE_GERENTE";

    private final InsumoService insumoService;

    public InsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'FUNCIONARIO')")
    @Operation(summary = "Lista os insumos ativos por nome. O Gerente pode usar ?incluirInativos=true")
    public List<InsumoResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInativos,
            Authentication autenticacao) {
        boolean gerente = ehGerente(autenticacao);
        return insumoService.listar(gerente && incluirInativos).stream()
                .map(insumo -> InsumoResponse.de(insumo, gerente))
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'FUNCIONARIO')")
    @Operation(summary = "Consulta um insumo (mesmo inativo). Funcionário recebe a resposta sem custo")
    public InsumoResponse buscar(@PathVariable Long id, Authentication autenticacao) {
        return InsumoResponse.de(insumoService.buscar(id), ehGerente(autenticacao));
    }

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Cadastra um insumo (só Gerente)")
    public ResponseEntity<InsumoGerenteResponse> criar(@Valid @RequestBody InsumoRequest requisicao) {
        Insumo criado = insumoService.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/insumos/" + criado.getId()))
                .body(InsumoGerenteResponse.de(criado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Edita nome, unidade de medida e custo unitário (só Gerente)")
    public InsumoGerenteResponse atualizar(@PathVariable Long id, @Valid @RequestBody InsumoRequest requisicao) {
        return InsumoGerenteResponse.de(insumoService.atualizar(id, requisicao));
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('GERENTE')")
    @Operation(summary = "Inativa o insumo, sem removê-lo (só Gerente)")
    public InsumoGerenteResponse inativar(@PathVariable Long id) {
        return InsumoGerenteResponse.de(insumoService.inativar(id));
    }

    private static boolean ehGerente(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream().anyMatch(autoridade -> ROLE_GERENTE.equals(autoridade.getAuthority()));
    }
}
