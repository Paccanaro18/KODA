package com.paccanaro.koda.question;

import com.paccanaro.koda.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Banco de questoes semeado")
class QuestionBankIntegrationTest extends AbstractIntegrationTest {

    private static final List<String> ALL_TYPES =
            List.of("single_choice", "multiple_select", "true_false", "ordering", "fill_in_blank");

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionVersionRepository questionVersionRepository;

    @Autowired
    private QuestionTypeRegistry typeRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("toda questao publicada passa na validacao do proprio tipo")
    void everyPublishedQuestionValidatesAgainstItsType() {
        List<Question> published = questionRepository.findAllByStatusAndQuestionTypeIn(QuestionStatus.PUBLISHED, ALL_TYPES);

        // 10 concepts x 3 questoes cada, seedados na V3 — se esse numero mudar,
        // a migration mudou e este teste precisa ser revisto de proposito.
        assertThat(published).hasSize(30);

        for (Question question : published) {
            QuestionVersion version = questionVersionRepository.findFirstByQuestionIdOrderByVersionDesc(question.getId())
                    .orElseThrow();
            JsonNode payload = objectMapper.readTree(version.getPayload());
            JsonNode correctAnswer = objectMapper.readTree(version.getCorrectAnswer());

            typeRegistry.get(question.getQuestionType()).validatePayload(payload, correctAnswer);
        }
    }

    @Test
    @DisplayName("duplicate_rate e zero no banco semeado")
    void duplicateRateIsZero() {
        List<QuestionVersion> allVersions = questionVersionRepository.findAll();

        Map<String, Long> countByHash = allVersions.stream()
                .collect(Collectors.groupingBy(v -> HexFormat.of().formatHex(v.getCanonicalHash()), Collectors.counting()));

        long duplicateGroups = countByHash.values().stream().filter(count -> count > 1).count();
        assertThat(duplicateGroups).isZero();
    }
}
