package com.paccanaro.koda.question;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Camadas 1+2 de deduplicacao (docs/architecture/03-estrategia-ia.md § 3):
 * hash do payload normalizado. Para conteudo curado a mao as duas camadas
 * colapsam numa so — a distincao entre "duplicata exata" e "hash canonico"
 * so importa quando ha reescrita por IA pra capturar (nomes de variavel
 * trocados, mesma questao reformulada), o que ainda nao existe (Fase 5).
 *
 * <p>Camada 3 (similaridade semantica via embedding) fica fora desta fase —
 * depende de infraestrutura de IA que ainda nao existe. Camada 4 (exposicao
 * do usuario) e {@link UserQuestionExposure}, usada em {@link QuestionService}.
 */
@Service
public class DuplicationService {

    /** SHA-256 do payload apos normalizar espacos e caixa — mesma logica usada na migration de seed. */
    public byte[] canonicalHash(String payload) {
        String normalized = payload.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e garantido pela JVM (java.security.MessageDigest javadoc); nunca deveria cair aqui.
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
