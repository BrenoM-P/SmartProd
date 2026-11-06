package br.com.smartprod.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.smartprod.auth.TokenService;

/**
 * Clique duplo no "Salvar": dois cadastros iguais ao mesmo tempo passam juntos pela conferência do
 * e-mail, e quem barra o segundo é a restrição única do banco. Sem {@code @Transactional} de propósito:
 * cada requisição precisa da própria transação, como em produção.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CadastroSimultaneoTest {

    private static final String EMAIL_GERENTE = "chefe.simultaneo@padaria.com";
    private static final String EMAIL_NOVO = "novo.simultaneo@padaria.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TokenService tokenService;

    @AfterEach
    void removerUsuariosDoTeste() {
        List.of(EMAIL_GERENTE, EMAIL_NOVO)
                .forEach(email -> usuarioRepository.findByEmail(email).ifPresent(usuarioRepository::delete));
    }

    @Test
    void doisCadastrosIguaisAoMesmoTempoRetornam201E409() throws Exception {
        Usuario chefe = usuarioRepository.save(new Usuario("Chefe", EMAIL_GERENTE, "hash", Perfil.GERENTE));
        String token = tokenService.emitir(chefe).valor();
        String corpo = """
                {"nome":"Novo","email":"%s","senha":"senha-forte-123","perfil":"FUNCIONARIO"}
                """.formatted(EMAIL_NOVO);

        CountDownLatch largada = new CountDownLatch(2);
        Callable<Integer> cadastrar = () -> {
            largada.countDown();
            largada.await();
            return mockMvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andReturn().getResponse().getStatus();
        };

        try (ExecutorService threads = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> respostas = threads.invokeAll(List.of(cadastrar, cadastrar));
            assertThat(List.of(respostas.get(0).get(), respostas.get(1).get())).containsExactlyInAnyOrder(201, 409);
        }
    }
}
