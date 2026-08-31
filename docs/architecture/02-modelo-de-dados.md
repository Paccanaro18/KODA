# KODA — Modelo de Dados Proposto

> Fase 0 (Architecture) · Status: proposta · Banco: PostgreSQL + extensão `pgvector`

## D. Schema inicial e relações

O modelo foi derivado do domínio, não copiado de uma lista. Três decisões estruturais o governam:

1. **`question_attempts` é append-only e é a fonte da verdade.** Todo o resto do modelo de conhecimento é projeção reconstruível a partir dele.
2. **Questões são versionadas e imutáveis após publicação.** Uma tentativa referencia a *versão* respondida, não a questão. Sem isso, editar uma questão corrompe retroativamente o histórico de aprendizado.
3. **Currículo e conteúdo são entidades separadas.** O currículo é curado e versionado no repositório; as questões são geradas e validadas. Confundir os dois deixaria a IA redefinir o que é ensinado.

## Identidade e privacidade

```
users
  id                    uuid pk
  email                 citext unique not null
  password_hash         text not null          -- Argon2id
  role                  enum('student','admin','moderator') not null default 'student'
  email_verified_at     timestamptz
  status                enum('active','suspended','deleted') not null
  created_at            timestamptz not null
  deleted_at            timestamptz            -- soft delete; purga por job

profiles
  user_id               uuid pk fk→users cascade
  display_name          text
  locale                text not null default 'pt-BR'
  timezone              text not null
  learning_goal         text                   -- ex.: 'backend', 'devops'
  daily_goal_minutes    int not null default 15
  prefers_reduced_motion boolean not null default false
```

`users` guarda o mínimo necessário. Dados de aprendizado ficam em tabelas separadas para permitir exportação e exclusão sem tocar na identidade — requisito de privacidade da seção 22.

## Currículo (curado, não gerado)

```
subjects        id, slug unique, title, description, icon, sort_order
topics          id, subject_id fk, slug, title, description, sort_order
                unique(subject_id, slug)
concepts        id, topic_id fk, slug, title, description,
                difficulty_min int, difficulty_max int,
                importance smallint,            -- peso na seleção
                learning_objectives jsonb,
                common_mistakes jsonb,
                estimated_minutes int
                unique(topic_id, slug)
concept_prerequisites
                concept_id fk, prerequisite_concept_id fk,
                strength enum('hard','soft')    -- hard bloqueia; soft apenas penaliza
                pk(concept_id, prerequisite_concept_id)
                CHECK (concept_id <> prerequisite_concept_id)
```

O grafo de pré-requisitos precisa ser acíclico. Ciclo é bug de conteúdo e deve quebrar o CI, não a experiência do aluno.

## Banco de questões

```
questions
  id                    uuid pk
  topic_id              uuid fk→topics
  question_type         text not null          -- resolvido pelo registry de tipos
  status                enum('draft','in_review','published','disabled','retired')
  current_version_id    uuid fk→question_versions
  created_at, updated_at timestamptz

question_versions                              -- IMUTÁVEL após publicação
  id                    uuid pk
  question_id           uuid fk→questions
  version               int not null
  payload               jsonb not null         -- forma validada por tipo
  correct_answer        jsonb not null         -- NUNCA serializado ao cliente
  explanation           text not null
  distractor_rationales jsonb                  -- por que cada alternativa erra
  declared_difficulty   smallint not null      -- prior do gerador (1..5)
  measured_difficulty   numeric(4,3)           -- empírico, recalibrado por job
  estimated_time_seconds int
  canonical_hash        bytea not null         -- dedup camada 2
  embedding             vector(768)            -- dedup camada 3
  quality_score         numeric(4,3)
  language              text
  created_at            timestamptz not null
  unique(question_id, version)

question_concepts
  question_version_id fk, concept_id fk, weight numeric
  pk(question_version_id, concept_id)

ai_generations                                 -- rastreabilidade (seção 20)
  id                    uuid pk
  question_id           uuid fk null
  model                 text not null
  prompt_version        text not null
  specification         jsonb not null
  raw_output            jsonb not null
  validation_results    jsonb not null         -- resultado de cada estágio
  outcome               enum('accepted','rejected_schema','rejected_answer',
                             'rejected_difficulty','rejected_duplicate',
                             'rejected_safety','rejected_quality')
  input_tokens, output_tokens int
  cost_usd              numeric(10,6)
  latency_ms            int
  created_at            timestamptz
```

Separar `questions` de `question_versions` é o que torna a curadoria segura: o admin edita criando uma nova versão, e as tentativas antigas continuam apontando para o que o aluno realmente viu.

## Aprendizado e evidência

```
question_attempts                              -- APPEND-ONLY, fonte da verdade
  id                    uuid pk
  user_id               uuid fk→users
  question_version_id   uuid fk→question_versions
  submitted_answer      jsonb not null
  is_correct            boolean not null
  error_type            enum('conceptual','syntax','careless',
                             'misunderstanding','knowledge_gap','guess') null
  response_time_ms      int not null
  hints_used            smallint not null default 0
  self_confidence       smallint null           -- 1..5, opcional
  difficulty_at_time    smallint not null
  created_at            timestamptz not null
  index (user_id, created_at desc)
  index (question_version_id)

skill_scores                                   -- PROJEÇÃO reconstruível
  user_id fk, concept_id fk
  mastery              numeric(4,3) not null    -- 0..1, estimativa de domínio
  stage                enum('unseen','recognizes','understands','applies',
                            'solves','masters') not null
  confidence           numeric(4,3) not null    -- certeza da própria estimativa
  attempts_total, attempts_correct int
  consecutive_errors   smallint
  last_practiced_at    timestamptz
  next_review_at       timestamptz              -- repetição espaçada
  decay_rate           numeric(4,3)             -- esquecimento estimado
  pk(user_id, concept_id)
  index (user_id, next_review_at)

user_question_exposure                         -- dedup camada 4
  user_id fk, question_id fk, times_seen int, last_seen_at timestamptz
  pk(user_id, question_id)

mistake_patterns
  user_id fk, concept_id fk, error_type, occurrences int, last_seen_at
  pk(user_id, concept_id, error_type)

learning_events                                -- trilha analítica append-only
  id, user_id, event_type, payload jsonb, created_at
```

`mastery` **não é XP.** XP mede esforço; `mastery` estima domínio e pode cair com o tempo por decaimento. Misturar os dois é o erro que transforma a plataforma em jogo de cliques.

O par `stage` + `mastery` implementa a progressão pedagógica da seção 3: `unseen → recognizes → understands → applies → solves → masters`. A transição de estágio exige evidência em dificuldade crescente, não apenas acúmulo de acertos.

## Progressão e gamificação

```
user_progress    user_id, topic_id, completion numeric, unlocked_at, mastered_at
xp_ledger        id, user_id, amount int, source, reference_id, created_at   -- append-only
streaks          user_id pk, current_days, longest_days, last_active_date, freezes_available
achievements     id, slug unique, title, description, icon, criteria jsonb
user_achievements user_id, achievement_id, unlocked_at, pk(user_id, achievement_id)
```

`xp_ledger` é um livro-razão append-only em vez de um contador — auditável e imune a corrida de escrita.

## Integridade e desempenho

- Todas as FKs explícitas, com `ON DELETE` deliberado por relação (cascade para dados do usuário, restrict para currículo).
- `CHECK` em toda faixa numérica (`difficulty BETWEEN 1 AND 5`, `mastery BETWEEN 0 AND 1`).
- Índice HNSW em `question_versions.embedding` para a busca de similaridade (`SCA-01`).
- Índice parcial em `questions` filtrando `status = 'published'` — é o predicado do caminho quente.
- `(user_id, next_review_at)` em `skill_scores` sustenta a fila de revisão espaçada.
- Migrations versionadas e forward-only; nunca editar migration aplicada (`DAT-03`).
- Auditoria em ações de admin e mudanças de `role`.

## Direito ao esquecimento

A exclusão de conta anonimiza `users`, apaga `profiles` e `skill_scores`, e **preserva** `question_attempts` de forma desassociada apenas se houver necessidade estatística agregada — decisão a formalizar na Fase 9 junto com a política de retenção.
