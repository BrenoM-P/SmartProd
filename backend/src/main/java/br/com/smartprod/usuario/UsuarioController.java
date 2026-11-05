package br.com.smartprod.usuario;

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

import br.com.smartprod.usuario.dto.UsuarioCriacaoRequest;
import br.com.smartprod.usuario.dto.UsuarioEdicaoRequest;
import br.com.smartprod.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Cadastro de usuários (RF002). Todas as rotas são só do Gerente (RNF-S002). */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('GERENTE')")
@Tag(name = "Usuários", description = "Cadastro de usuários e colaboradores (RF002) — só Gerente")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um usuário (409 se o e-mail já existe)")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioCriacaoRequest requisicao) {
        Usuario criado = usuarioService.criar(requisicao);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.getId()))
                .body(UsuarioResponse.de(criado));
    }

    @GetMapping
    @Operation(summary = "Lista os usuários por nome; ?ativo=true ou ?ativo=false filtra")
    public List<UsuarioResponse> listar(@RequestParam(required = false) Boolean ativo) {
        return usuarioService.listar(ativo).stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um usuário")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return UsuarioResponse.de(usuarioService.buscar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita nome, e-mail e perfil; a senha só muda se for informada")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioEdicaoRequest requisicao,
            Authentication autenticacao) {
        return UsuarioResponse.de(usuarioService.atualizar(id, requisicao, idDoSolicitante(autenticacao)));
    }

    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativa o usuário, sem removê-lo (não vale para a própria conta)")
    public UsuarioResponse inativar(@PathVariable Long id, Authentication autenticacao) {
        return UsuarioResponse.de(usuarioService.inativar(id, idDoSolicitante(autenticacao)));
    }

    /** O nome da autenticação é o id do usuário (claim {@code sub} do token); fora disso, não há id. */
    private static Long idDoSolicitante(Authentication autenticacao) {
        try {
            return Long.valueOf(autenticacao.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
