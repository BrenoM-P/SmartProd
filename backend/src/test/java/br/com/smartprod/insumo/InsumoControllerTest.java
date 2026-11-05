package br.com.smartprod.insumo;

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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.smartprod.auth.TokenService;
import br.com.smartprod.usuario.Perfil;
import br.com.smartprod.usuario.Usuario;
import br.com.smartprod.usuario.UsuarioRepository;

/** Cadastro de insumos (RF004) de ponta a ponta. O Funcionário lê, mas nunca vê o custo (RNF-S002). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // cada teste desfaz os insumos que criou
class InsumoControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InsumoRepository insumoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private ObjectMapper objectMapper;

    // ---- acesso

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/insumos")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioNaoCriaNemEditaNemInativa() throws Exception {
        Insumo insumo = salvar("Farinha", UnidadeMedida.KG, "6.50", true);
        String corpo = json("nome", "X", "unidadeMedida", "KG", "custoUnitario", new BigDecimal("1.00"));

        mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/insumos/" + insumo.getId()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/insumos/" + insumo.getId() + "/inativar")).andExpect(status().isForbidden());
    }

    // ---- Funcionário: leitura sem custo

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioListaSemCustoESoOsAtivos() throws Exception {
        salvar("Farinha de trigo", UnidadeMedida.KG, "6.50", true);
        salvar("Insumo velho", UnidadeMedida.KG, "9.99", false);

        mockMvc.perform(get("/api/insumos?incluirInativos=true")) // o parâmetro não vale para Funcionário
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Farinha de trigo")))
                .andExpect(jsonPath("$[*].nome", not(hasItem("Insumo velho"))))
                .andExpect(jsonPath("$[0].custoUnitario").doesNotExist())
                .andExpect(jsonPath("$[0].ativo").doesNotExist())
                .andExpect(content().string(not(containsString("custoUnitario"))))
                .andExpect(content().string(not(containsString("6.5"))));
    }

    @Test
    @WithMockUser(roles = "FUNCIONARIO")
    void funcionarioConsultaPorIdSemCustoMesmoDeInsumoInativo() throws Exception {
        Insumo inativo = salvar("Insumo velho", UnidadeMedida.UN, "9.99", false);

        mockMvc.perform(get("/api/insumos/" + inativo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Insumo velho"))
                .andExpect(jsonPath("$.unidadeMedida").value("UN"))
                .andExpect(jsonPath("$.custoUnitario").doesNotExist());
    }

    @Test
    void funcionarioComTokenRealTambemNaoVeOCusto() throws Exception {
        salvar("Farinha de trigo", UnidadeMedida.KG, "6.50", true);
        Usuario funcionario = usuarioRepository.saveAndFlush(
                new Usuario("Beto", "beto.insumo@padaria.com", "hash", Perfil.FUNCIONARIO));
        String token = tokenService.emitir(funcionario).valor();

        mockMvc.perform(get("/api/insumos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Farinha de trigo")))
                .andExpect(content().string(not(containsString("custoUnitario"))));
    }

    // ---- Gerente: CRUD

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteCadastraInsumoEVeOCusto() throws Exception {
        mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "  Farinha de trigo ", "unidadeMedida", "KG", "custoUnitario",
                        new BigDecimal("6.5"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/insumos/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Farinha de trigo"))
                .andExpect(jsonPath("$.unidadeMedida").value("KG"))
                .andExpect(jsonPath("$.custoUnitario").value(6.5))
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(insumoRepository.findAllByOrderByNomeAsc()).extracting(Insumo::getNome)
                .contains("Farinha de trigo");
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void aceitaCustoPequenoPorGramaComQuatroCasas() throws Exception {
        mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Fermento", "unidadeMedida", "G", "custoUnitario", new BigDecimal("0.0065"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.custoUnitario").value(0.0065));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void mostraOCustoSemZerosSobrandoDoBanco() throws Exception {
        Insumo insumo = salvar("Manteiga", UnidadeMedida.KG, "40.5000", true);

        mockMvc.perform(get("/api/insumos/" + insumo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"custoUnitario\":40.50")))
                .andExpect(content().string(not(containsString("40.5000"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void dadosInvalidosRetornam400() throws Exception {
        corpoInvalido(json("nome", "  ", "unidadeMedida", "KG", "custoUnitario", new BigDecimal("1")));
        corpoInvalido(json("nome", "Sal", "unidadeMedida", "XX", "custoUnitario", new BigDecimal("1")));
        corpoInvalido(json("nome", "Sal", "unidadeMedida", null, "custoUnitario", new BigDecimal("1")));
        corpoInvalido(json("nome", "Sal", "unidadeMedida", "KG", "custoUnitario", null));
        corpoInvalido(json("nome", "Sal", "unidadeMedida", "KG", "custoUnitario", new BigDecimal("-0.01")));
        corpoInvalido(json("nome", "Sal", "unidadeMedida", "KG", "custoUnitario", new BigDecimal("1.12345")));
        corpoInvalido(json("nome", "Sal", "unidadeMedida", "KG", "custoUnitario", new BigDecimal("123456789")));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteListaSoOsAtivosPorPadraoETodosComIncluirInativos() throws Exception {
        salvar("Ativo teste", UnidadeMedida.KG, "1.00", true);
        salvar("Inativo teste", UnidadeMedida.KG, "2.00", false);

        mockMvc.perform(get("/api/insumos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Ativo teste")))
                .andExpect(jsonPath("$[*].nome", not(hasItem("Inativo teste"))))
                .andExpect(jsonPath("$[0].custoUnitario").exists())
                .andExpect(jsonPath("$[0].ativo").exists());
        mockMvc.perform(get("/api/insumos?incluirInativos=true")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Ativo teste")))
                .andExpect(jsonPath("$[*].nome", hasItem("Inativo teste")));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void consultaPorIdEDevolve404QuandoNaoExiste() throws Exception {
        Insumo insumo = salvar("Açúcar", UnidadeMedida.KG, "5.00", true);

        mockMvc.perform(get("/api/insumos/" + insumo.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.custoUnitario").value(5.0));
        mockMvc.perform(get("/api/insumos/999999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Insumo não encontrado."));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void editaNomeUnidadeECusto() throws Exception {
        Insumo insumo = salvar("Farinha", UnidadeMedida.KG, "6.50", true);

        mockMvc.perform(put("/api/insumos/" + insumo.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "Farinha integral", "unidadeMedida", "G", "custoUnitario",
                        new BigDecimal("0.0080"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Farinha integral"))
                .andExpect(jsonPath("$.unidadeMedida").value("G"))
                .andExpect(jsonPath("$.custoUnitario").value(0.008))
                .andExpect(jsonPath("$.ativo").value(true));

        mockMvc.perform(put("/api/insumos/999999999").contentType(MediaType.APPLICATION_JSON)
                .content(json("nome", "X", "unidadeMedida", "G", "custoUnitario", BigDecimal.ONE)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void inativaSemRemoverEOInsumoSaiDaListaPadrao() throws Exception {
        Insumo insumo = salvar("Insumo para inativar", UnidadeMedida.UN, "1.00", true);

        mockMvc.perform(patch("/api/insumos/" + insumo.getId() + "/inativar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(get("/api/insumos")).andExpect(
                jsonPath("$[*].nome", not(hasItem("Insumo para inativar"))));
        mockMvc.perform(get("/api/insumos?incluirInativos=true")).andExpect(
                jsonPath("$[*].nome", hasItem("Insumo para inativar")));
        mockMvc.perform(get("/api/insumos/" + insumo.getId())).andExpect(status().isOk());
        mockMvc.perform(patch("/api/insumos/999999999/inativar")).andExpect(status().isNotFound());
    }

    // ---- apoio

    private void corpoInvalido(String corpo) throws Exception {
        mockMvc.perform(post("/api/insumos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest());
    }

    private Insumo salvar(String nome, UnidadeMedida unidade, String custo, boolean ativo) {
        Insumo insumo = new Insumo(nome, unidade, new BigDecimal(custo));
        if (!ativo) {
            insumo.inativar();
        }
        return insumoRepository.saveAndFlush(insumo);
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
