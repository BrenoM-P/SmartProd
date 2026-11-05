package br.com.smartprod.usuario.dto;

import java.time.Instant;

import br.com.smartprod.usuario.Perfil;
import br.com.smartprod.usuario.Usuario;

/** Usuário como a API devolve. Nunca inclui a senha nem o hash. */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo, Instant criadoEm) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                usuario.isAtivo(), usuario.getCriadoEm());
    }
}
