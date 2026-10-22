package br.com.tiagocarvalho.conhecimento.suporte;

import br.com.tiagocarvalho.conhecimento.documento.Documento;
import br.com.tiagocarvalho.conhecimento.documento.RepositorioDocumentos;
import br.com.tiagocarvalho.conhecimento.documento.StatusDocumento;
import br.com.tiagocarvalho.conhecimento.pergunta.CacheDeRespostas;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: sobe PostgreSQL (com pgvector) e RabbitMQ de verdade em contêineres Docker.
 *
 * Os contêineres são iniciados uma única vez (bloco static) e compartilhados por todas as classes de teste,
 * junto com o mesmo contexto do Spring. Por isso cada teste usa dados únicos (UUID no conteúdo e nos e-mails).
 *
 * Só a IA é falsa (EmbeddingsFalsos e ModeloFalso): testes não podem depender de rede nem gastar créditos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegracaoBase.ConfiguracaoDeTeste.class)
public abstract class IntegracaoBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    static final RabbitMQContainer RABBIT = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management"));

    static {
        POSTGRES.start();
        RABBIT.start();
    }

    /** Aponta a aplicação para os contêineres (as portas são aleatórias a cada execução). */
    @DynamicPropertySource
    static void apontarParaContainers(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.rabbitmq.host", RABBIT::getHost);
        registro.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registro.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registro.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    protected static final String EMAIL_ADMIN = "admin@teste.com";
    protected static final String SENHA_ADMIN = "senha-do-admin-123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ModeloFalso modeloFalso;

    @Autowired
    protected CacheDeRespostas cache;

    @Autowired
    protected RepositorioDocumentos documentos;

    @BeforeEach
    void reiniciarEstadoCompartilhado() {
        modeloFalso.reiniciar();
        cache.limpar();
    }

    @TestConfiguration
    static class ConfiguracaoDeTeste {

        @Bean
        @Primary
        EmbeddingsFalsos embeddingsFalsos() {
            return new EmbeddingsFalsos(768);
        }

        @Bean
        @Primary
        ModeloFalso modeloFalso() {
            return new ModeloFalso();
        }
    }

    // ---------- atalhos usados pelos testes ----------

    protected String json(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected String entrar(String email, String senha) throws Exception {
        String corpo = json(mvc.perform(post("/api/auth/entrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk()));
        return JsonPath.read(corpo, "$.token");
    }

    protected String tokenAdmin() throws Exception {
        return entrar(EMAIL_ADMIN, SENHA_ADMIN);
    }

    /** Cria um usuário comum novo e devolve o token dele. */
    protected String tokenUsuarioNovo() throws Exception {
        String email = "usuario-" + UUID.randomUUID() + "@teste.com";
        String corpo = json(mvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuária Teste\",\"email\":\"%s\",\"senha\":\"senha-forte-123\"}".formatted(email)))
                .andExpect(status().isCreated()));
        return JsonPath.read(corpo, "$.token");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected ResultActions enviarArquivo(String token, String nomeArquivo, String titulo, String conteudo) throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", nomeArquivo, "text/plain", conteudo.getBytes(StandardCharsets.UTF_8));
        return mvc.perform(multipart("/api/documentos")
                .file(arquivo)
                .param("titulo", titulo)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    protected UUID enviarDocumento(String token, String nomeArquivo, String titulo, String conteudo) throws Exception {
        String corpo = json(enviarArquivo(token, nomeArquivo, titulo, conteudo).andExpect(status().isCreated()));
        return UUID.fromString(JsonPath.read(corpo, "$.id"));
    }

    protected Documento aguardarStatus(UUID id, StatusDocumento esperado) {
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200))
                .until(() -> documentos.findById(id).map(Documento::getStatus).orElse(null) == esperado);
        return documentos.findById(id).orElseThrow();
    }

    protected ResultActions perguntar(String token, String pergunta) throws Exception {
        return mvc.perform(post("/api/perguntas")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pergunta\":\"%s\"}".formatted(pergunta)));
    }
}
