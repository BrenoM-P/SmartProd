package br.com.smartprod.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import br.com.smartprod.auth.TokenService.TokenEmitido;
import br.com.smartprod.auth.dto.LoginRequest;
import br.com.smartprod.auth.dto.LoginResponse;
import br.com.smartprod.usuario.Usuario;
import br.com.smartprod.usuario.UsuarioRepository;

/** Regras do login (RF001): confere e-mail e senha e emite o token. */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
            TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
    }

    /**
     * @throws org.springframework.security.core.AuthenticationException se o e-mail ou a senha estiverem
     *         errados ou o usuário estiver inativo (o controller devolve 401 sem dizer qual dos casos).
     */
    public LoginResponse login(LoginRequest requisicao) {
        Authentication autenticacao = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(requisicao.email(), requisicao.senha()));

        Usuario usuario = usuarioRepository.findByEmail(autenticacao.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado"));
        TokenEmitido token = tokenService.emitir(usuario);

        return new LoginResponse(token.valor(), LoginResponse.TIPO_BEARER, token.expiraEm(),
                new LoginResponse.UsuarioLogado(usuario.getId(), usuario.getNome(), usuario.getPerfil()));
    }
}
