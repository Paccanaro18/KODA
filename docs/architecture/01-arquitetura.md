# KODA — Arquitetura Proposta

> Fase 0 (Architecture) · Status: proposta

## C. Visão geral

O princípio que organiza tudo: **a IA é um componente controlado, não o sistema.** A decisão pedagógica é determinística e auditável; a IA apenas produz conteúdo candidato, sempre sujeito a validação.

```
                        ┌──────────────────────────┐
                        │   Frontend (Web/Mobile)  │
                        └────────────┬─────────────┘
                                     │ HTTPS + JWT
                        ┌────────────▼─────────────┐
                        │      Backend / API       │
                        │  authn · authz · RBAC    │
                        │  rate limit · validação  │
                        └────────────┬─────────────┘
                                     │
        ┌────────────────┬───────────┼────────────┬─────────────────┐
        │                │           │            │                 │
┌───────▼──────┐ ┌───────▼──────┐ ┌──▼─────────┐ ┌▼──────────┐ ┌────▼─────┐
│  Adaptive    │ │  Question    │ │ Curriculum │ │Gamification│ │  Admin   │
│  Learning    │ │  Engine      │ │  Service   │ │  Service   │ │ Service  │
│  Engine      │ │              │ │            │ │            │ │          │
│ DETERMINÍS-  │ │ serve/valida │ │ tópicos,   │ │ XP, streak │ │ curadoria│
│ TICO         │ │ dedup        │ │ pré-reqs   │ │ conquistas │ │ métricas │
└───────┬──────┘ └───────┬──────┘ └──┬─────────┘ └┬──────────┘ └────┬─────┘
        │                │           │            │                 │
        └────────────────┴───────────┼────────────┴─────────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │   PostgreSQL + pgvector  │
                        │   Redis (cache/limites)  │
                        └──────────────────────────┘

                    ═══ caminho ASSÍNCRONO (nunca no request do aluno) ═══

┌──────────────┐   ┌──────────────┐   ┌──────────┐   ┌────────────────────┐
│ Queue/Workers│──▶│  AI Gateway  │──▶│   LLM    │──▶│Validation Pipeline │
│ pré-geração  │   │ retry·budget │   │ provider │   │ 7 estágios         │
└──────────────┘   │ circuit brkr │   └──────────┘   └─────────┬──────────┘
                   └──────────────┘                             │
┌──────────────┐                                     ┌──────────▼─────────┐
│   Sandbox    │◀── execução isolada de código ──────│   Question Bank    │
│ container    │                                     │ (review_status)    │
└──────────────┘                                     └────────────────────┘

              Observabilidade (logs · métricas · traces · audit) — transversal
```

## Componentes

### Frontend
SPA/SSR responsiva, mobile-first e acessível. Não contém nenhuma regra de autorização — apenas reflete o que o servidor autoriza. Consome design tokens centralizados (ver `04-design-system.md`). Não recebe a resposta correta de uma questão antes de o aluno responder.

### Backend / API
Fronteira única de autenticação, autorização (RBAC + escopo por dono), validação de input, rate limiting e headers seguros. Não contém lógica pedagógica — orquestra os serviços de domínio.

### Adaptive Learning Engine — o coração
**Determinístico, puro e sem I/O.** Recebe o perfil de conhecimento e devolve uma `QuestionSpecification` mais um `selection_reason` legível.

Entradas consideradas: `knowledge_level`, `recent_accuracy`, `historical_accuracy`, `difficulty`, `time_since_last_review`, `mistake_patterns`, `topic_importance`, `prerequisites`, `recent_exposure`, `question_similarity`, `learning_goal`.

Decide: qual tópico praticar, qual dificuldade, qual tipo de questão, quando revisar, quando introduzir conceito novo e quando subir ou descer a dificuldade. Ser puro é o que o torna exaustivamente testável — requisito explícito da Fase 4.

### Question Engine
Serve questões do banco atendendo à especificação. Executa o pipeline de deduplicação, registra a exposição e corrige as respostas. **A correção é sempre server-side e determinística** para tipos objetivos; a IA nunca decide se o aluno acertou uma múltipla escolha.

### Curriculum Service
Grafo de conceitos com pré-requisitos. Responde "o aluno está pronto para este conceito?" e alimenta o mapa de aprendizado. É conteúdo curado, versionado no repositório — não gerado por IA.

### AI Gateway
Única porta de saída para o LLM. Concentra timeout, retry com backoff exponencial, circuit breaker, rate limit, controle de orçamento, versionamento de prompt e de modelo, fallback, logging seguro e métricas de custo. Nenhuma outra parte do código fala com o provedor diretamente — é isso que permite trocar de provedor depois.

### Sandbox
Serviço isolado para executar código do aluno e validar código gerado. Container efêmero, sem rede, filesystem read-only, sem privilégios, com limites de CPU, memória, processos e tempo. Fisicamente separado do processo da API.

### Queue / Workers
Pré-geração de questões, cálculo de embeddings, recalibração de dificuldade e recomputação de projeções de skill. Tudo que é caro ou lento vive aqui, fora do request do aluno.

### Observabilidade
Logs estruturados sem dados pessoais nem segredos, métricas, traces distribuídos e trilha de auditoria. Métricas de negócio obrigatórias: `question_generation_failure`, `question_validation_failure`, `duplicate_rate`, `question_quality`, `answer_accuracy`, `ai_latency`, `ai_cost`, `sandbox_failures`, `api_latency`, `authentication_failures`.

### Admin
Curadoria de questões (aprovar, editar, desativar), inspeção de gerações de IA com rastreabilidade completa, métricas de qualidade e revisão de conteúdo problemático. Acesso restrito por RBAC e integralmente auditado.

## Fluxo canônico de uma questão

```
Perfil do estudante
      ↓
Adaptive Learning Engine        ← determinístico, testável, explicável
      ↓
Question Specification
      ↓
Question Bank (busca)  ──── encontrou? ───▶ serve ao aluno
      ↓ não encontrou
enfileira geração (assíncrona)  ──▶ AI Gateway ──▶ LLM
      ↓                                              ↓
serve o melhor candidato disponível          Validation Pipeline
                                                     ↓
                                             Question Bank
```

O aluno nunca espera pelo LLM. Essa é a diferença entre um protótipo e um produto.

## Fronteiras não negociáveis

1. A IA não decide nível, dificuldade nem progressão — o Engine decide.
2. A IA não tem acesso a banco, filesystem ou infraestrutura.
3. Toda saída de IA é validada antes de persistir e antes de ser exibida.
4. Toda autorização é verificada no backend.
5. Nenhum código do usuário executa no processo da aplicação.
6. Nenhum segredo ou dado pessoal entra em um prompt.
