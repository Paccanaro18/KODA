package com.paccanaro.koda;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

/**
 * Base dos testes de integracao.
 *
 * <p>Usa Postgres e Redis reais em container, nao substitutos em memoria: as
 * garantias que este projeto depende — indice unico, CHECK constraints, cascata,
 * expiracao no Redis — simplesmente nao existem em H2 nem em mocks. Um teste que
 * passa contra um banco falso nao prova nada sobre producao.
 *
 * <p>A imagem e a mesma pgvector usada em desenvolvimento, para que o schema
 * seja exercitado no mesmo motor onde vai rodar.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres");

    @SuppressWarnings("resource")
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("koda_test")
            .withUsername("koda")
            .withPassword("koda_test_password")
            .withReuse(true);

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Zera contadores de rate limit e de tentativas de login antes de cada teste.
     *
     * <p>Sem isso a suite fica acoplada a propria ordem: todos os testes chegam
     * do mesmo IP, os contadores se acumulam entre eles, e a partir de certo
     * ponto qualquer teste passa a receber 429 por causa do que os anteriores
     * fizeram. O sintoma seria uma falha intermitente e dificil de rastrear.
     */
    @BeforeEach
    void resetRateLimitCounters() {
        Set<String> keys = redisTemplate.keys("koda:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // Segredo exclusivo de teste. O de producao vem do ambiente e nunca do codigo.
        registry.add("koda.security.jwt.secret",
                () -> "segredo-de-teste-apenas-com-mais-de-32-bytes-para-hs256");
    }
}
