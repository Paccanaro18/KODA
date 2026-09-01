package com.paccanaro.koda.curriculum;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Recusa o grafo de pre-requisitos se houver ciclo. Um ciclo tornaria os
 * concepts envolvidos eternamente "locked" — nenhum teria pre-requisito
 * satisfazivel.
 *
 * <p>DFS simples em vez de uma biblioteca de grafo: o grafo e pequeno (dezenas
 * de arestas curadas a mao) e nao ha lib de grafo no classpath — adicionar uma
 * so para isso nao se paga.
 */
@Component
public class CurriculumGraphValidator {

    /** @throws IllegalStateException se o grafo tiver ciclo */
    public void validate(List<ConceptPrerequisite> edges) {
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        for (ConceptPrerequisite edge : edges) {
            adjacency.computeIfAbsent(edge.getConceptId(), k -> new ArrayList<>())
                    .add(edge.getPrerequisiteConceptId());
        }

        Set<UUID> visited = new HashSet<>();
        Set<UUID> inStack = new HashSet<>();

        for (UUID node : adjacency.keySet()) {
            if (!visited.contains(node) && hasCycle(node, adjacency, visited, inStack)) {
                throw new IllegalStateException(
                        "Grafo de pre-requisitos do curriculo tem ciclo envolvendo o concept " + node);
            }
        }
    }

    private boolean hasCycle(UUID node, Map<UUID, List<UUID>> adjacency,
                              Set<UUID> visited, Set<UUID> inStack) {
        visited.add(node);
        inStack.add(node);

        for (UUID neighbor : adjacency.getOrDefault(node, List.of())) {
            if (inStack.contains(neighbor)) {
                return true;
            }
            if (!visited.contains(neighbor) && hasCycle(neighbor, adjacency, visited, inStack)) {
                return true;
            }
        }

        inStack.remove(node);
        return false;
    }
}
