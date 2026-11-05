package br.com.smartprod.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.smartprod.auth.dto.LoginRequest;
import br.com.smartprod.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login e token de acesso (RF001)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica por e-mail e senha e devolve o token JWT")
    @ApiResponse(responseCode = "200", description = "Login realizado")
    @ApiResponse(responseCode = "400", description = "E-mail ou senha ausentes", content = @Content)
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos, ou usuário inativo",
            content = @Content)
    public LoginResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return authService.login(requisicao);
    }
}
