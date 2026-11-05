package br.com.smartprod.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.smartprod.auth.TokenService;

/** Cadastro de usuários (RF002) de ponta a ponta: token real, perfil conferido no banco, H2 e BCrypt. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // cada teste desfaz os usuários que criou
class UsuarioControllerTest {

    private static final String SENHA = "senha-de-teste-123";
    private static final String MSG_EMAIL_EM_USO = "Já existe um usuário com este e-mail.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private ObjectMapper objectMapper;

    private Usuario chefe;
    private String tokenChefe;

    @BeforeEach
    void criarGerenteLogado() {
        chefe = salvar("chefe.teste@padaria.com", Perfil.GERENTE, true);
        tokenChefe = tokenService.emitir(chefe).valor();
    }

    // ---- acesso

    @Test
    void semTokenRetorna401() throws Exception {
        chamar(get("/api/usuarios"), null).andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioRecebe403EmTodasAsOperacoes() throws Exception {
        Usuario funcionario = salvar("func.teste@padaria.com", Perfil.FUNCIONARIO, true);
        String token = tokenService.emitir(funcionario).valor();
        String corpo = json("nome", "X", "email", "x@padaria.com", "senha", "senha-forte-1", "perfil", "FUNCIONARIO");

        chamar(get("/api/usuarios"), token).andExpect(status().isForbidden());
        chamar(get("/api/usuarios/" + chefe.getId()), token).andExpect(status().isForbidden());
        chamar(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo), token)
                .andExpect(status().isForbidden());
        chamar(put("/api/usuarios/" + chefe.getId()).contentType(MediaType.APPLICATION_JSON).content(corpo), token)
                .andExpect(status().isForbidden());
        chamar(patch("/api/usuarios/" + chefe.getId() + "/inativar"), token).andExpect(status().isForbidden());
    }

    // ---- criar

    @Test
    void gerenteCadastraUsuarioComSenhaEmBcryptSemDevolverSenha() throws Exception {
        chamar(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Ana Souza", "email", "Ana.Souza@Padaria.com", "senha", "senha-forte-1",
                        "perfil", "FUNCIONARIO")), tokenChefe)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/usuarios/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana.souza@padaria.com"))
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));

        Usuario salvo = usuarioRepository.findByEmail("ana.souza@padaria.com").orElseThrow();
        assertThat(salvo.getSenhaHash()).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches("senha-forte-1", salvo.getSenhaHash())).isTrue();
        login("ana.souza@padaria.com", "senha-forte-1").andExpect(status().isOk());
    }

    @Test
    void emailDuplicadoRetorna409MesmoComMaiusculasDiferentes() throws Exception {
        salvar("dup.teste@padaria.com", Perfil.FUNCIONARIO, true);

        chamar(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Outro", "email", "DUP.Teste@Padaria.com", "senha", "senha-forte-1",
                        "perfil", "FUNCIONARIO")), tokenChefe)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(MSG_EMAIL_EM_USO));
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        corpoInvalido(json("nome", "Ana", "email", "isso-nao-e-email", "senha", "senha-forte-1", "perfil", "GERENTE"));
        corpoInvalido(json("nome", "Ana", "email", "ana@padaria.com", "senha", "curta", "perfil", "GERENTE"));
        corpoInvalido(json("nome", "Ana", "email", "ana@padaria.com", "senha", "senha-forte-1", "perfil", null));
        corpoInvalido(json("nome", "  ", "email", "ana@padaria.com", "senha", "senha-forte-1", "perfil", "GERENTE"));
        corpoInvalido(json("nome", "Ana", "email", "ana@padaria.com", "senha", "senha-forte-1", "perfil", "ADMIN"));
    }

    // ---- consultar

    @Test
    void listaOsUsuariosEFiltraPorAtivo() throws Exception {
        salvar("ativo.teste@padaria.com", Perfil.FUNCIONARIO, true);
        salvar("inativo.teste@padaria.com", Perfil.FUNCIONARIO, false);

        chamar(get("/api/usuarios"), tokenChefe).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("ativo.teste@padaria.com")))
                .andExpect(jsonPath("$[*].email", hasItem("inativo.teste@padaria.com")))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andExpect(content().string(not(containsString("senhaHash"))));
        chamar(get("/api/usuarios?ativo=false"), tokenChefe).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("inativo.teste@padaria.com")))
                .andExpect(jsonPath("$[*].email", not(hasItem("ativo.teste@padaria.com"))));
        chamar(get("/api/usuarios?ativo=true"), tokenChefe).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("ativo.teste@padaria.com")))
                .andExpect(jsonPath("$[*].email", not(hasItem("inativo.teste@padaria.com"))));
    }

    @Test
    void consultaUmUsuarioEDevolve404QuandoNaoExiste() throws Exception {
        chamar(get("/api/usuarios/" + chefe.getId()), tokenChefe).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("chefe.teste@padaria.com"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"));

        chamar(get("/api/usuarios/999999999"), tokenChefe).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado."));
    }

    // ---- editar

    @Test
    void editaNomeEmailEPerfilSemMexerNaSenhaQuandoNaoInformada() throws Exception {
        Usuario beto = salvar("beto.teste@padaria.com", Perfil.FUNCIONARIO, true);

        chamar(put("/api/usuarios/" + beto.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Beto Novo", "email", "Beto.Novo@padaria.com", "perfil", "GERENTE")), tokenChefe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Beto Novo"))
                .andExpect(jsonPath("$.email").value("beto.novo@padaria.com"))
                .andExpect(jsonPath("$.perfil").value("GERENTE"));

        login("beto.novo@padaria.com", SENHA).andExpect(status().isOk());
    }

    @Test
    void trocaASenhaQuandoInformada() throws Exception {
        Usuario beto = salvar("beto.teste@padaria.com", Perfil.FUNCIONARIO, true);

        chamar(put("/api/usuarios/" + beto.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Beto", "email", "beto.teste@padaria.com", "perfil", "FUNCIONARIO", "senha",
                        "nova-senha-123")), tokenChefe)
                .andExpect(status().isOk());

        login("beto.teste@padaria.com", "nova-senha-123").andExpect(status().isOk());
        login("beto.teste@padaria.com", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void emailDeOutroUsuarioNaEdicaoRetorna409MasOProprioEmailEAceito() throws Exception {
        salvar("ana.teste@padaria.com", Perfil.FUNCIONARIO, true);
        Usuario beto = salvar("beto.teste@padaria.com", Perfil.FUNCIONARIO, true);

        chamar(put("/api/usuarios/" + beto.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Beto", "email", "ANA.teste@padaria.com", "perfil", "FUNCIONARIO")), tokenChefe)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(MSG_EMAIL_EM_USO));

        chamar(put("/api/usuarios/" + beto.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Beto Silva", "email", "beto.teste@padaria.com", "perfil", "FUNCIONARIO")),
                tokenChefe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Beto Silva"));
    }

    @Test
    void gerenteNaoMudaOProprioPerfilMasPodeEditarOProprioNome() throws Exception {
        chamar(put("/api/usuarios/" + chefe.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Chefe", "email", "chefe.teste@padaria.com", "perfil", "FUNCIONARIO")),
                tokenChefe)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Você não pode alterar o próprio perfil."));

        chamar(put("/api/usuarios/" + chefe.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Chefe Novo", "email", "chefe.teste@padaria.com", "perfil", "GERENTE")),
                tokenChefe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Chefe Novo"));
    }

    // ---- inativar

    @Test
    void inativaSemRemoverEOUsuarioPerdeOAcessoNaHora() throws Exception {
        Usuario funcionario = salvar("func.teste@padaria.com", Perfil.FUNCIONARIO, true);
        String tokenFuncionario = tokenService.emitir(funcionario).valor();
        chamar(get("/api/usuarios"), tokenFuncionario).andExpect(status().isForbidden()); // token vale: 403, não 401

        chamar(patch("/api/usuarios/" + funcionario.getId() + "/inativar"), tokenChefe)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        chamar(get("/api/usuarios/" + funcionario.getId()), tokenChefe).andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false)); // continua cadastrado
        login("func.teste@padaria.com", SENHA).andExpect(status().isUnauthorized());
        chamar(get("/api/usuarios"), tokenFuncionario).andExpect(status().isUnauthorized()); // o token antigo caiu
    }

    @Test
    void naoInativaAPropriaConta() throws Exception {
        chamar(patch("/api/usuarios/" + chefe.getId() + "/inativar"), tokenChefe)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Você não pode inativar a própria conta."));

        chamar(get("/api/usuarios"), tokenChefe).andExpect(status().isOk()); // segue ativo
    }

    @Test
    void inativarUsuarioInexistenteRetorna404() throws Exception {
        chamar(patch("/api/usuarios/999999999/inativar"), tokenChefe).andExpect(status().isNotFound());
    }

    // ---- apoio

    private void corpoInvalido(String corpo) throws Exception {
        chamar(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo), tokenChefe)
                .andExpect(status().isBadRequest());
    }

    private ResultActions chamar(MockHttpServletRequestBuilder requisicao, String token) throws Exception {
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "senha", senha)));
    }

    private Usuario salvar(String email, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario("Teste", email, passwordEncoder.encode(SENHA), perfil);
        if (!ativo) {
            usuario.inativar();
        }
        return usuarioRepository.saveAndFlush(usuario);
    }

    /** Monta um JSON a partir de pares chave, valor (aceita valores nulos). */
    private String json(Object... pares) throws Exception {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put((String) pares[i], pares[i + 1]);
        }
        return objectMapper.writeValueAsString(mapa);
    }
}
