package br.com.smartprod.usuario.dto;

import br.com.smartprod.usuario.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code PUT /api/usuarios/{id}} (RF002). A {@code senha} é opcional: omitida (ou nula),
 * a senha atual é mantida; informada, precisa ter de 8 a 128 caracteres.
 */
public record UsuarioEdicaoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Perfil perfil,
        @Size(min = 8, max = 128) String senha) {

    /** Evita que a senha apareça em log se o objeto for impresso por engano. */
    @Override
    public String toString() {
        return "UsuarioEdicaoRequest[***]";
    }
}
