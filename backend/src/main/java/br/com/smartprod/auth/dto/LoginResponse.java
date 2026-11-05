package br.com.smartprod.auth.dto;

import java.time.Instant;

import br.com.smartprod.usuario.Perfil;

/**
 * Resposta de {@code POST /api/auth/login}, conforme o contrato do docs/09.
 * O token vai nas demais chamadas como {@code Authorization: Bearer <token>}.
 */
public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioLogado usuario) {

    public static final String TIPO_BEARER = "Bearer";

    /** Dados do usuário que o frontend guarda na sessão. Nunca inclui a senha. */
    public record UsuarioLogado(Long id, String nome, Perfil perfil) {
    }
}
