-- KODA - Fase 5: geracao de questoes por IA (AI Gateway, pipeline de 7
-- estagios, fila assincrona). Referencias: docs/architecture/03-estrategia-ia.md
-- (geracao, validacao, custo), docs/architecture/02-modelo-de-dados.md
-- (ai_generations). Esta migration e imutavel apos aplicada; correcoes vem
-- em uma nova versao.
--
-- specification/raw_output/validation_results sao coluna `text` com JSON
-- serializado, mesma decisao da V3 (evita depender de mapeamento nativo
-- jsonb do Hibernate 7 / Jackson 3 sem verificar).

-- ---------------------------------------------------------------------------
-- generation_requests: fila de pre-geracao. O aluno nunca aciona isto no
-- caminho do request (ARC-01) — so o worker assincrono le daqui.
-- ---------------------------------------------------------------------------
CREATE TABLE generation_requests (
    id                 uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id         uuid        NOT NULL REFERENCES concepts (id) ON DELETE CASCADE,
    question_type      text        NOT NULL,
    target_difficulty  integer     NOT NULL,
    status             text        NOT NULL DEFAULT 'PENDING',
    created_at         timestamptz NOT NULL DEFAULT now(),
    processed_at       timestamptz,

    CONSTRAINT generation_requests_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED')),
    CONSTRAINT generation_requests_difficulty_check
        CHECK (target_difficulty BETWEEN 1 AND 5)
);

CREATE INDEX generation_requests_pending_idx ON generation_requests (created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- ai_generations: rastreabilidade completa de toda chamada ao LLM, aceita ou
-- rejeitada — inclusive o motivo exato da rejeicao (secao 20 da estrategia
-- de IA). question_id fica nulo quando a geracao e rejeitada antes de
-- persistir uma questao.
-- ---------------------------------------------------------------------------
CREATE TABLE ai_generations (
    id                   uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    generation_request_id uuid       REFERENCES generation_requests (id) ON DELETE SET NULL,
    question_id          uuid        REFERENCES questions (id) ON DELETE SET NULL,
    model                text        NOT NULL,
    prompt_version       text        NOT NULL,
    specification        text        NOT NULL,
    raw_output           text,
    validation_results   text        NOT NULL,
    outcome              text        NOT NULL,
    input_tokens         integer,
    output_tokens        integer,
    cost_usd             numeric(10,6),
    latency_ms           integer,
    created_at           timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ai_generations_outcome_check CHECK (outcome IN (
        'ACCEPTED', 'REJECTED_SCHEMA', 'REJECTED_ANSWER', 'REJECTED_DIFFICULTY',
        'REJECTED_CURRICULUM', 'REJECTED_DUPLICATE', 'REJECTED_SAFETY',
        'REJECTED_QUALITY', 'REJECTED_PROVIDER_ERROR'
    ))
);

CREATE INDEX ai_generations_created_idx ON ai_generations (created_at DESC);
CREATE INDEX ai_generations_outcome_idx ON ai_generations (outcome);
