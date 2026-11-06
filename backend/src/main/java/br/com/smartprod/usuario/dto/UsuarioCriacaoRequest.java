package br.com.smartprod.usuario.dto;

import br.com.smartprod.usuario.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /api/usuarios} (RF002). */
public record UsuarioCriacaoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @NotNull Perfil perfil) {

    /** Evita que a senha apareça em log se o objeto for impresso por engano. */
    @Override
    public String toString() {
        return "UsuarioCriacaoRequest[***]";
    }
}
