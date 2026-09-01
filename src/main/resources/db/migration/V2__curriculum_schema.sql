-- KODA - Fase 2: curriculo (grafo de conceitos e pre-requisitos).
-- Referencias: docs/architecture/02-modelo-de-dados.md (curriculo curado e
-- versionado no repo, separado do conteudo gerado). Esta migration e imutavel
-- apos aplicada; correcoes vem em uma nova versao.

-- ---------------------------------------------------------------------------
-- subjects / topics / concepts: hierarquia curada do curriculo.
-- ---------------------------------------------------------------------------
CREATE TABLE subjects (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    slug          text        NOT NULL,
    name          text        NOT NULL,
    display_order integer     NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT subjects_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE UNIQUE INDEX subjects_slug_key ON subjects (slug);

CREATE TABLE topics (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id    uuid        NOT NULL REFERENCES subjects (id) ON DELETE CASCADE,
    slug          text        NOT NULL,
    name          text        NOT NULL,
    description   text,
    display_order integer     NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT topics_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE UNIQUE INDEX topics_subject_slug_key ON topics (subject_id, slug);
CREATE INDEX topics_subject_idx ON topics (subject_id, display_order);

CREATE TABLE concepts (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id      uuid        NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    slug          text        NOT NULL,
    title         text        NOT NULL,
    display_order integer     NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT concepts_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);

CREATE UNIQUE INDEX concepts_topic_slug_key ON concepts (topic_id, slug);
CREATE INDEX concepts_topic_idx ON concepts (topic_id, display_order);

-- ---------------------------------------------------------------------------
-- concept_prerequisites: arestas do grafo. Um concept so fica "available" para
-- o aluno quando todo pre-requisito estiver completed/mastered — calculado na
-- aplicacao a cada leitura, nunca persistido aqui. O CurriculumGraphValidator
-- recusa a aplicacao subir se este grafo tiver ciclo (um ciclo tornaria os
-- conceitos envolvidos eternamente "locked").
-- ---------------------------------------------------------------------------
CREATE TABLE concept_prerequisites (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id               uuid NOT NULL REFERENCES concepts (id) ON DELETE CASCADE,
    prerequisite_concept_id  uuid NOT NULL REFERENCES concepts (id) ON DELETE CASCADE,

    CONSTRAINT concept_prerequisites_no_self_loop CHECK (concept_id <> prerequisite_concept_id)
);

CREATE UNIQUE INDEX concept_prerequisites_edge_key
    ON concept_prerequisites (concept_id, prerequisite_concept_id);
CREATE INDEX concept_prerequisites_concept_idx ON concept_prerequisites (concept_id);

-- ---------------------------------------------------------------------------
-- user_concept_progress: evidencia real de pratica. So existe linha aqui para
-- o que o aluno de fato tentou — "locked" e "available" nunca sao persistidos,
-- sao sempre calculados a partir da ausencia de linha + grafo de pre-requisito.
-- Fica vazia ate a Fase 3/4 (banco de questoes, engine adaptativo) escrever.
-- ---------------------------------------------------------------------------
CREATE TABLE user_concept_progress (
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    concept_id        uuid        NOT NULL REFERENCES concepts (id) ON DELETE CASCADE,
    state             text        NOT NULL,
    progress_percent  integer     NOT NULL DEFAULT 0,
    updated_at        timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT user_concept_progress_state_check
        CHECK (state IN ('ACTIVE', 'COMPLETED', 'MASTERED', 'NEEDS_REVIEW')),
    CONSTRAINT user_concept_progress_percent_check
        CHECK (progress_percent BETWEEN 0 AND 100)
);

CREATE UNIQUE INDEX user_concept_progress_user_concept_key ON user_concept_progress (user_id, concept_id);

-- ---------------------------------------------------------------------------
-- Seed: conteudo curado inicial. Duas trilhas completas dentro do subject
-- "Programacao", formalizando o que ja existia como mock na tela /aprender.
-- ---------------------------------------------------------------------------
INSERT INTO subjects (slug, name, display_order) VALUES
    ('programacao', 'Programação', 1);

INSERT INTO topics (subject_id, slug, name, description, display_order)
SELECT s.id, t.slug, t.name, t.description, t.display_order
FROM subjects s
CROSS JOIN (VALUES
    ('fundamentos-da-linguagem', 'Fundamentos da linguagem', 'Tipos, controle de fluxo e funções', 1),
    ('estruturas-de-dados', 'Estruturas de dados', 'Listas, mapas e complexidade', 2)
) AS t(slug, name, description, display_order)
WHERE s.slug = 'programacao';

INSERT INTO concepts (topic_id, slug, title, display_order)
SELECT t.id, c.slug, c.title, c.display_order
FROM topics t
CROSS JOIN (VALUES
    ('tipos-primitivos', 'Tipos primitivos', 1),
    ('condicionais', 'Condicionais', 2),
    ('lacos', 'Laços', 3),
    ('funcoes', 'Funções', 4),
    ('escopo', 'Escopo', 5),
    ('recursao', 'Recursão', 6)
) AS c(slug, title, display_order)
WHERE t.slug = 'fundamentos-da-linguagem';

INSERT INTO concepts (topic_id, slug, title, display_order)
SELECT t.id, c.slug, c.title, c.display_order
FROM topics t
CROSS JOIN (VALUES
    ('arrays', 'Arrays', 1),
    ('listas-ligadas', 'Listas ligadas', 2),
    ('mapas', 'Mapas', 3),
    ('complexidade', 'Complexidade', 4)
) AS c(slug, title, display_order)
WHERE t.slug = 'estruturas-de-dados';

-- Cadeia linear dentro de cada trilha, mais a costura entre as duas: o
-- primeiro conceito de "Estruturas de dados" exige o ultimo de "Fundamentos".
INSERT INTO concept_prerequisites (concept_id, prerequisite_concept_id)
SELECT c.id, p.id
FROM (VALUES
    ('condicionais', 'tipos-primitivos'),
    ('lacos', 'condicionais'),
    ('funcoes', 'lacos'),
    ('escopo', 'funcoes'),
    ('recursao', 'escopo'),
    ('arrays', 'recursao'),
    ('listas-ligadas', 'arrays'),
    ('mapas', 'listas-ligadas'),
    ('complexidade', 'mapas')
) AS edge(concept_slug, prereq_slug)
JOIN concepts c ON c.slug = edge.concept_slug
JOIN concepts p ON p.slug = edge.prereq_slug;
