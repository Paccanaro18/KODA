# KODA — Roadmap

> Fase 0 (Architecture) · Status: proposta

Cada fase tem critérios objetivos de conclusão. Uma fase só está concluída quando **todos** os critérios são verificáveis por comando, não por opinião.

A "Definição de Pronto" vale para toda funcionalidade em todas as fases: implementada · tipada · validada · testada · segura · observável · documentada · integrada · sem segredos expostos · sem erros críticos.

---

## Phase 0 — Architecture ← **atual**

Auditoria, riscos, arquitetura, modelo de dados, estratégia de IA, roadmap e ADRs.

**Concluída quando:**
- [x] Auditoria do projeto entregue com diagnóstico e registro de riscos classificado
- [x] Arquitetura de componentes definida com fronteiras explícitas
- [x] Modelo de dados inicial proposto com justificativa das decisões estruturais
- [x] Estratégia de IA definida (geração, validação, deduplicação, custo, falhas)
- [x] Roadmap com critérios objetivos por fase
- [x] **ADR-0001 (stack) aceito** — Spring Boot (Java 21) + Next.js
- [ ] Documentação versionada no repositório

## Phase 1 — Authentication + Core

Fundação do backend, autenticação, autorização e o esqueleto do frontend com design tokens.

**Concluída quando:**
- [x] Registro, login, refresh e logout implementados com hashing Argon2id
- [x] RBAC com autorização server-side (`@EnableMethodSecurity`, default deny)
- [x] Rate limiting e proteção contra brute force implementados
- [x] Secure headers e CSP configurados
- [x] Migrations versionadas (Flyway `V1`)
- [x] Docker Compose sobe Postgres + Redis com um comando
- [x] Health check e métricas expostos via Actuator
- [x] **Suíte de testes executada e verde — 19/19** (fluxo de auth, autorização/IDOR `SEC-03`, brute force `SEC-06`)
- [x] Migrations validadas rodando do zero em banco limpo (Flyway + Postgres real via Testcontainers)
- [x] CI com build, testes, scanner de segredos e scan de dependências
- [x] Design tokens implementados; dark e light mode funcionais
- [x] Biblioteca de componentes base com estados explícitos; lint, tipos e build verdes

> `ENV-02` **resolvido** em 2026-08-31: o WSL2 foi instalado e o Docker Desktop
> passou a subir o engine Linux, destravando Testcontainers e `docker compose`.

## Phase 2 — Curriculum

Grafo de conceitos, pré-requisitos e mapa de aprendizado.

**Concluída quando:**
- [x] Schema de subjects, topics, concepts e pré-requisitos migrado (`V2__curriculum_schema.sql`)
- [x] Conteúdo curado inicial para ao menos 2 trilhas completas (Fundamentos da linguagem, Estruturas de dados — 10 concepts)
- [x] Validador de grafo detecta ciclos e quebra o CI (`CurriculumGraphValidator` + `CurriculumGraphStartupCheck`, aplicação recusa subir com ciclo)
- [x] API de currículo com estados: locked, available, active, completed, mastered, needs-review (`GET /api/v1/curriculum/map`)
- [x] Mapa de aprendizado navegável e responsivo (`/aprender` ligado à API real)
- [ ] Regra de pré-requisito impede conteúdo avançado sem base, **exceto em modo diagnóstico** — a regra em si está implementada e testada; o modo diagnóstico (pular pré-requisito num teste de nivelamento) não existe ainda, é feature própria maior que esta fase

## Phase 3 — Question Engine

Banco de questões, tipos extensíveis, correção e deduplicação.

**Concluída quando:**
- [x] Registry de tipos permite adicionar um tipo novo sem alterar o núcleo (`MNT-01`) — `QuestionTypeRegistry`
- [x] Ao menos 5 tipos implementados com render, validate e score (single_choice, multiple_select, true_false, ordering, fill_in_blank)
- [x] Correção determinística server-side; gabarito nunca sai antes da submissão (testado em `PracticeFlowIntegrationTest`)
- [x] `question_attempts` append-only registrando os sinais usados pelo engine
- [x] Questões versionadas e imutáveis após publicação (`DAT-02`)
- [x] Feedback rico: correção, motivo, conceito testado, por que as alternativas erram
- [ ] Deduplicação: camadas 1, 2 e 4 operantes com `duplicate_rate` medido; **camada 3 (similaridade semântica via `pgvector`) não implementada** — depende de infraestrutura de embeddings
- [ ] `question_attempts` ainda não grava `error_type`, `self_confidence` nem `hints_used` (colunas existem; a classificação de erro é da Fase 4/8)

## Phase 4 — Adaptive Learning

O Adaptive Learning Engine determinístico. É a fase de maior densidade de testes.

**Concluída quando:**
- [x] Engine é puro, sem I/O, e recebe o perfil como entrada (`AdaptiveEngine`; usa `CurriculumConcept` próprio para não depender de entidade JPA)
- [x] `selection_reason` legível retornado e exibido ao aluno (`UX-02`) — `PracticeQuestionResponse.selectionReason`
- [x] Priorização: revisão > foco atual > conceito novo, respeitando pré-requisitos
- [x] Ajuste de dificuldade por precisão recente, com amostra mínima para não oscilar
- [ ] **Parte das entradas da seção 4 consideradas** — usa precisão recente/histórica, dificuldade, erros consecutivos e pré-requisitos; **faltam** `time_since_last_review`, `mistake_patterns`, `topic_importance` e `learning_goal`
- [ ] Progressão de estágio de 6 níveis (unseen → … → masters) — hoje usa os 4 estados de `ProgressState` (ACTIVE, COMPLETED, MASTERED, NEEDS_REVIEW)
- [ ] Repetição espaçada com `next_review_at` e decaimento — não implementada
- [ ] Classificação de erro alimentando as próximas seleções — não implementada
- [ ] Testes property-based garantindo invariantes

## Phase 5 — AI Generation

AI Gateway e pipeline de validação.

**Concluída quando:**
- [x] AI Gateway é a única saída para o LLM, com timeout, retry, backoff, circuit breaker, rate limit e orçamento (`AiGatewayClient` + `AnthropicAiGatewayClient`, Resilience4j; orçamento diário em `AiBudgetGuard`)
- [x] Prompts versionados; `prompt_version` e `model` gravados em toda geração (`QuestionGenerationPrompts.VERSION`, `ai_generations`)
- [x] Pipeline de 7 estágios operante, com motivo de rejeição registrado (`ValidationPipeline`, `validation_results` + `outcome`)
- [x] Geração assíncrona por fila; **nenhuma chamada de LLM no request do aluno** (`ARC-01`) — `generation_requests` + `GenerationWorker` agendado
- [x] Custo e tokens observáveis por geração e por modelo (`input_tokens`, `output_tokens`, `cost_usd`, `latency_ms`)
- [x] Testes de prompt injection direto e indireto (`SEC-02`)
- [x] Nenhum segredo ou dado pessoal em prompt, verificado por teste (`QuestionGenerationPromptsTest`)
- [x] Fallback validado: provedor indisponível não afeta o aluno (teste de integração cobre queda do provedor + sessão de prática seguindo normal)
- [ ] Custo agregado **por tópico e por prompt** — hoje o custo é observável por geração e por modelo; o corte por tópico depende do painel da Fase 8
- [ ] Validação cruzada por segunda passagem (modelo crítico adversarial) — previsto na estratégia de IA, não implementado nesta fase
- [ ] Fluxo de revisão humana: questão gerada nasce `IN_REVIEW` e **não há ainda tela de admin** para promovê-la a `PUBLISHED` (Fase 8)

## Phase 6 — Code Sandbox

Execução isolada de código.

**Concluída quando:**
- [ ] Execução em container efêmero, fora do processo da API
- [ ] Sem rede, filesystem read-only, `cap-drop ALL`, usuário não-root, seccomp
- [ ] Limites de CPU, memória, processos e timeout duro aplicados
- [ ] Quota por usuário e fila dedicada (`SCA-02`)
- [ ] Suite de testes de escape: rede, filesystem, fork bomb, loop infinito, consumo de memória
- [ ] `sandbox_failures` monitorado

## Phase 7 — Gamification

XP, streak, conquistas e progressão — sem mecânicas manipulativas.

**Concluída quando:**
- [ ] `xp_ledger` append-only e auditável
- [ ] XP derivado de evidência de domínio; revisão espaçada pontua (`UX-01`)
- [ ] Streak com política de recuperação justa
- [ ] Conquistas com critérios declarativos
- [ ] Microinterações implementadas respeitando `prefers-reduced-motion`
- [ ] Nenhuma mecânica que otimize cliques em detrimento de aprendizado

## Phase 8 — Analytics

Instrumentação, qualidade de conteúdo e insight para o aluno.

**Concluída quando:**
- [ ] Todas as métricas de negócio da seção 23 instrumentadas
- [ ] Recalibração empírica de dificuldade rodando (`AI-03`)
- [ ] Questão com taxa de acerto anômala rebaixada automaticamente
- [ ] Painel de qualidade de conteúdo no admin
- [ ] Dashboard do aluno mostrando evolução, lacunas e recomendação explicada
- [ ] Traces distribuídos cobrindo o caminho crítico

## Phase 9 — Security Hardening

**Concluída quando:**
- [ ] Revisão contra OWASP Top 10 documentada, com evidência por item
- [ ] Teste automatizado de autorização em todos os endpoints
- [ ] Testes de XSS, injection, SSRF e CSRF onde aplicável
- [ ] Dependency scanning e scanner de segredos bloqueando o CI
- [ ] Política de privacidade implementada: exportar dados, excluir conta, retenção definida
- [ ] Audit logging de ações sensíveis
- [ ] Encryption in transit; at rest onde apropriado
- [ ] Threat model revisado e riscos `CRITICAL` todos mitigados com evidência

## Phase 10 — Production

**Concluída quando:**
- [ ] Deploy reproduzível com rollback testado
- [ ] Backup e restore **testados por restauração real**, não apenas configurados
- [ ] Alertas acionáveis com dono definido
- [ ] Runbook de incidente escrito
- [ ] Teste de carga no caminho crítico
- [ ] Acessibilidade auditada: contraste, navegação por teclado, focus states, screen reader, touch targets
- [ ] Performance mobile validada em dispositivo real
