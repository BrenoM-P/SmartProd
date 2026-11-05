package br.com.smartprod.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.JsonPath;

import br.com.smartprod.usuario.Perfil;
import br.com.smartprod.usuario.Usuario;
import br.com.smartprod.usuario.UsuarioRepository;

/**
 * Autorização por perfil (D1-BE-02): o Spring lê o token Bearer, confere o usuário no banco e o
 * {@code @PreAuthorize} decide. Como este teste não depende de endpoints de negócio, ele registra um
 * controller só dele.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AcessoPorPerfilTest.ControllerDeTeste.class)
@Transactional
class AcessoPorPerfilTest {

    private static final String LOGADO = "/api/teste-acesso/logado";
    private static final String SO_GERENTE = "/api/teste-acesso/gerente";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---- 401: sem token ou com token que não vale

    @Test
    void semTokenRetorna401() throws Exception {
        chamar(LOGADO, null).andExpect(status().isUnauthorized());
        chamar(SO_GERENTE, null).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenQueNaoEUmJwtRetorna401() throws Exception {
        chamar(LOGADO, "isso-nao-e-um-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void cabecalhoQueNaoEBearerRetorna401() throws Exception {
        mockMvc.perform(get(LOGADO).header("Authorization", "Basic dXN1YXJpbzpzZW5oYQ=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAssinadoComOutroSegredoRetorna401() throws Exception {
        JwtProperties outras = new JwtProperties("outro-segredo-com-mais-de-32-caracteres!!", 60);
        TokenService estranho = new TokenService(new JwtConfig().jwtEncoder(outras), outras);

        chamar(LOGADO, estranho.emitir(salvar(Perfil.GERENTE)).valor()).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenVencidoRetorna401() throws Exception {
        Clock doisDiasAtras = Clock.fixed(Instant.now().minus(Duration.ofDays(2)), ZoneOffset.UTC);
        TokenService antigo = new TokenService(jwtEncoder, jwtProperties, doisDiasAtras);

        chamar(LOGADO, antigo.emitir(salvar(Perfil.GERENTE)).valor()).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOutroEmissorRetorna401() throws Exception {
        Usuario usuario = salvar(Perfil.GERENTE);

        chamar(LOGADO, emitirComClaims("outro-sistema", String.valueOf(usuario.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioQueNaoExisteRetorna401() throws Exception {
        chamar(LOGADO, emitirComClaims(TokenService.EMISSOR, "999999999")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSemIdNumericoNoSubjectRetorna401() throws Exception {
        chamar(LOGADO, emitirComClaims(TokenService.EMISSOR, "abc")).andExpect(status().isUnauthorized());
    }

    // ---- 200 e 403: o perfil decide

    @Test
    void tokenValidoDeGerenteAcessaTudo() throws Exception {
        String token = tokenService.emitir(salvar(Perfil.GERENTE)).valor();

        chamar(LOGADO, token).andExpect(status().isOk()).andExpect(content().string("ok"));
        chamar(SO_GERENTE, token).andExpect(status().isOk()).andExpect(content().string("ok"));
    }

    @Test
    void funcionarioAcessaRotaDeLogadoMasNaoRotaDeGerente() throws Exception {
        String token = tokenService.emitir(salvar(Perfil.FUNCIONARIO)).valor();

        chamar(LOGADO, token).andExpect(status().isOk());
        chamar(SO_GERENTE, token).andExpect(status().isForbidden());
    }

    // ---- o banco manda: mudanças valem na hora, sem esperar o token expirar

    @Test
    void mudancaDePerfilNoBancoValeNaHora() throws Exception {
        Usuario usuario = salvar(Perfil.GERENTE);
        String token = tokenService.emitir(usuario).valor();
        chamar(SO_GERENTE, token).andExpect(status().isOk());

        usuario.atualizar(usuario.getNome(), usuario.getEmail(), Perfil.FUNCIONARIO);
        usuarioRepository.saveAndFlush(usuario);

        chamar(SO_GERENTE, token).andExpect(status().isForbidden());
        chamar(LOGADO, token).andExpect(status().isOk());
    }

    @Test
    void usuarioInativadoPerdeOAcessoNaHoraMesmoComTokenValido() throws Exception {
        Usuario usuario = salvar(Perfil.FUNCIONARIO);
        String token = tokenService.emitir(usuario).valor();
        chamar(LOGADO, token).andExpect(status().isOk());

        usuario.inativar();
        usuarioRepository.saveAndFlush(usuario);

        chamar(LOGADO, token).andExpect(status().isUnauthorized());
    }

    // ---- fluxo completo e detalhes

    @Test
    void tokenDoLoginRealFuncionaNasDemaisRotas() throws Exception {
        usuarioRepository.saveAndFlush(new Usuario("Maria Teste", "maria.teste@padaria.com",
                passwordEncoder.encode("senha-de-teste-123"), Perfil.GERENTE));

        String resposta = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria.teste@padaria.com\",\"senha\":\"senha-de-teste-123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(resposta, "$.token");

        chamar(SO_GERENTE, token).andExpect(status().isOk());
    }

    @Test
    void loginIgnoraTokenVencidoOuInvalidoNoCabecalho() throws Exception {
        usuarioRepository.saveAndFlush(new Usuario("Maria Teste", "maria.teste@padaria.com",
                passwordEncoder.encode("senha-de-teste-123"), Perfil.GERENTE));

        mockMvc.perform(post("/api/auth/login")
                .header("Authorization", "Bearer token-velho-guardado-no-navegador")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"maria.teste@padaria.com\",\"senha\":\"senha-de-teste-123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void withMockUserDeGerenteServeParaTestarControllers() throws Exception {
        mockMvc.perform(get(SO_GERENTE)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void withMockUserDeFuncionarioRecebe403NaRotaDeGerente() throws Exception {
        mockMvc.perform(get(SO_GERENTE)).andExpect(status().isForbidden());
    }

    // ---- apoio

    private ResultActions chamar(String caminho, String token) throws Exception {
        MockHttpServletRequestBuilder requisicao = get(caminho);
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(requisicao);
    }

    private Usuario salvar(Perfil perfil) {
        return usuarioRepository.saveAndFlush(
                new Usuario("Teste", UUID.randomUUID() + "@padaria.com", "hash", perfil));
    }

    /** Token válido na assinatura e no prazo, mas com o emissor e o subject que o teste escolher. */
    private String emitirComClaims(String emissor, String subject) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .subject(subject)
                .issuedAt(agora)
                .expiresAt(agora.plus(Duration.ofHours(1)))
                .claim(TokenService.CLAIM_PERFIL, "GERENTE")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    /** Controller só do teste: uma rota para qualquer usuário logado e outra só para Gerente. */
    @RestController
    @RequestMapping("/api/teste-acesso")
    public static class ControllerDeTeste {

        @GetMapping("/logado")
        public String logado() {
            return "ok";
        }

        @GetMapping("/gerente")
        @PreAuthorize("hasRole('GERENTE')")
        public String gerente() {
            return "ok";
        }
    }
}
