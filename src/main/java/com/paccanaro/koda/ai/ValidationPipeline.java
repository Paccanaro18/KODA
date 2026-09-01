package com.paccanaro.koda.ai;

import com.paccanaro.koda.config.KodaAiProperties;
import com.paccanaro.koda.question.DuplicationService;
import com.paccanaro.koda.question.QuestionTypeHandler;
import com.paccanaro.koda.question.QuestionTypeRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Os 7 estagios de validacao (docs/architecture/03-estrategia-ia.md secao 2).
 * Qualquer reprovacao descarta o candidato e devolve o motivo, que e gravado
 * em {@code ai_generations}.
 *
 * <p>Este e o componente que impede {@code AI-01} — questao com resposta
 * errada ou ambigua chegando ao aluno, o pior dano que esta plataforma pode
 * causar. Por isso ele e deliberadamente conservador: na duvida, rejeita.
 * Rejeitar demais custa tokens; aceitar de menos ensina errado.
 *
 * <p>Deterministico e sem I/O de rede — recebe tudo pronto e devolve um
 * veredito. Mesma disciplina do {@code AdaptiveEngine}, pelo mesmo motivo:
 * e o que o torna exaustivamente testavel sem chamar LLM nenhum.
 */
@Component
public class ValidationPipeline {

    /**
     * Termos que nunca deveriam aparecer numa questao de programacao basica
     * (estagio 6). Lista curta e literal de proposito: e uma rede de seguranca
     * contra o modo de falha obvio, nao um classificador de seguranca —
     * a defesa real contra conteudo perigoso e o sandbox da Fase 6, que nunca
     * executa nada disto de verdade.
     */
    private static final List<String> UNSAFE_MARKERS = List.of(
            "rm -rf", "drop table", "drop database", "truncate table",
            "sk-ant-", "api_key", "apikey", "password=", "senha=",
            "curl http", "wget http", "eval(", "exec(", "system(",
            "/etc/passwd", "ssh-rsa", "begin private key");

    private static final int MIN_EXPLANATION_LENGTH = 20;
    private static final int MAX_DIFFICULTY_DRIFT = 1;

    /** Ver {@link #stemsOf}: palavras curtas nao viram radical, e o corte tira sufixo de flexao. */
    private static final int MIN_STEM_LENGTH = 4;
    private static final int STEM_TRIM = 3;

    private final QuestionTypeRegistry typeRegistry;
    private final DuplicationService duplicationService;
    private final ObjectMapper objectMapper;
    private final KodaAiProperties properties;

    public ValidationPipeline(QuestionTypeRegistry typeRegistry, DuplicationService duplicationService,
                              ObjectMapper objectMapper, KodaAiProperties properties) {
        this.typeRegistry = typeRegistry;
        this.duplicationService = duplicationService;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * @param existingHashes hashes canonicos ja no banco para o mesmo concept (estagio 5)
     */
    public ValidationResult validate(GeneratedQuestion candidate, GenerationSpecification specification,
                                     Set<String> existingHashes) {
        JsonNode payload;
        JsonNode correctAnswer;

        // Estagio 1 — schema: estrutura, tipos, campos obrigatorios.
        try {
            payload = objectMapper.readTree(candidate.payloadJson());
            correctAnswer = objectMapper.readTree(candidate.correctAnswerJson());
        } catch (JacksonException e) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_SCHEMA, "1-schema",
                    "payloadJson ou correctAnswerJson nao e JSON valido: " + e.getOriginalMessage());
        }
        if (candidate.explanation() == null || candidate.explanation().strip().length() < MIN_EXPLANATION_LENGTH) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_SCHEMA, "1-schema",
                    "explicacao ausente ou curta demais para ensinar algo");
        }

        // Estagio 2 — correcao: a resposta certa e de fato valida para o tipo, e e UNICA.
        // O mais importante do sistema: e ele que impede AI-01. Reusa exatamente o
        // mesmo validador que o banco curado ja usa, entao conteudo gerado e conteudo
        // curado passam pelo mesmo criterio.
        QuestionTypeHandler handler;
        try {
            handler = typeRegistry.get(specification.questionType());
        } catch (RuntimeException e) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_SCHEMA, "1-schema",
                    "tipo de questao desconhecido: " + specification.questionType());
        }
        try {
            handler.validatePayload(payload, correctAnswer);
        } catch (IllegalArgumentException e) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_ANSWER, "2-correcao", e.getMessage());
        }
        // A propria resposta declarada precisa ser aceita pelo corretor. Se o score
        // da resposta correta der falso, a questao e insolucionavel — pega
        // incoerencia entre payload e correctAnswer que o validate nao alcanca.
        if (!handler.score(payload, correctAnswer, correctAnswer)) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_ANSWER, "2-correcao",
                    "a resposta declarada como correta nao passa no proprio corretor do tipo");
        }

        // Estagio 3 — dificuldade: bate com a faixa pedida?
        int declared = candidate.declaredDifficulty();
        if (declared < 1 || declared > 5) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_DIFFICULTY, "3-dificuldade",
                    "dificuldade declarada fora da faixa 1..5: " + declared);
        }
        if (Math.abs(declared - specification.targetDifficulty()) > MAX_DIFFICULTY_DRIFT) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_DIFFICULTY, "3-dificuldade",
                    "dificuldade declarada %d distante demais da pedida %d"
                            .formatted(declared, specification.targetDifficulty()));
        }

        // Estagio 4 — curriculo: a questao testa o conceito pedido?
        // Heuristica deliberadamente fraca: sem embeddings (Fase 5 nao os tem),
        // so da pra checar presenca textual. Serve pra pegar o caso grosseiro de
        // questao completamente fora do tema; nao pretende ser mais que isso.
        String haystack = normalize(candidate.payloadJson() + " " + candidate.explanation());
        boolean mentionsConcept = stemsOf(specification.conceptTitle()).stream().anyMatch(haystack::contains);
        if (!mentionsConcept) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_CURRICULUM, "4-curriculo",
                    "questao nao menciona nada do conceito alvo: " + specification.conceptTitle());
        }

        // Estagio 5 — deduplicacao: camadas 1+2 (hash canonico do payload).
        String hash = HexFormat.of().formatHex(duplicationService.canonicalHash(candidate.payloadJson()));
        if (existingHashes.contains(hash)) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_DUPLICATE, "5-dedup",
                    "payload identico a uma questao ja existente neste concept");
        }

        // Estagio 6 — seguranca: conteudo inseguro, comando destrutivo, segredo.
        String safetyHaystack = normalize(candidate.payloadJson() + " " + candidate.correctAnswerJson()
                + " " + candidate.explanation());
        for (String marker : UNSAFE_MARKERS) {
            if (safetyHaystack.contains(marker)) {
                return ValidationResult.rejected(GenerationOutcome.REJECTED_SAFETY, "6-seguranca",
                        "conteudo contem marcador inseguro: " + marker);
            }
        }

        // Estagio 7 — qualidade: limiar minimo de confianca.
        BigDecimal confidence = BigDecimal.valueOf(candidate.confidence());
        if (confidence.compareTo(properties.quality().minConfidence()) < 0) {
            return ValidationResult.rejected(GenerationOutcome.REJECTED_QUALITY, "7-qualidade",
                    "confianca %s abaixo do minimo %s".formatted(confidence, properties.quality().minConfidence()));
        }

        return ValidationResult.approve();
    }

    /**
     * Radicais aproximados das palavras do titulo do concept. Comparar a palavra
     * inteira nao funciona em portugues: o titulo diz "Condicionais" e a questao
     * diz "condicional" — flexao de numero e genero varia o tempo todo. Cortar
     * os ultimos caracteres cobre esse caso sem trazer um stemmer de verdade,
     * que seria peso demais para uma heuristica que existe so pra pegar questao
     * grosseiramente fora do tema.
     */
    private static List<String> stemsOf(String title) {
        return java.util.Arrays.stream(normalize(title).split("\\s+"))
                .filter(word -> word.length() >= MIN_STEM_LENGTH)
                .map(word -> word.substring(0, Math.max(MIN_STEM_LENGTH, word.length() - STEM_TRIM)))
                .toList();
    }

    /** Minusculas e sem acento — "Laços" e "lacos" precisam casar. */
    private static String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
