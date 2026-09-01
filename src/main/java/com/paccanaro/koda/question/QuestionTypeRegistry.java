package com.paccanaro.koda.question;

import com.paccanaro.koda.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolve o {@link QuestionTypeHandler} certo por {@code question_type}. O
 * Spring injeta todo bean que implementa a interface — adicionar um tipo novo
 * e so declarar um {@code @Component} novo, nada aqui muda (MNT-01).
 */
@Component
public class QuestionTypeRegistry {

    private final Map<String, QuestionTypeHandler> handlersByType;

    public QuestionTypeRegistry(List<QuestionTypeHandler> handlers) {
        this.handlersByType = handlers.stream()
                .collect(Collectors.toMap(QuestionTypeHandler::typeName, Function.identity()));
    }

    public QuestionTypeHandler get(String questionType) {
        QuestionTypeHandler handler = handlersByType.get(questionType);
        if (handler == null) {
            throw ApiException.notFound("Tipo de questao '" + questionType + "'");
        }
        return handler;
    }
}
