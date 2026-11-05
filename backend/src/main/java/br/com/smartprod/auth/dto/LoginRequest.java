package br.com.smartprod.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/auth/login}. */
public record LoginRequest(
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(max = 128) String senha) {

    /** Evita que e-mail e senha apareçam em log se o objeto for impresso por engano. */
    @Override
    public String toString() {
        return "LoginRequest[***]";
    }
}
