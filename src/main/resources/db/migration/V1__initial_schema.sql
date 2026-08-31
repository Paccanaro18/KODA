-- KODA - Fase 1: identidade, perfil e sessoes.
-- Referencias de risco: SEC-03 (IDOR), SEC-06 (brute force), DAT-03 (migrations forward-only).
-- Esta migration e imutavel apos aplicada. Correcoes vem em uma nova versao.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- users: apenas identidade e credencial. Dados de aprendizado ficam separados
-- para permitir exportacao e exclusao sem tocar na identidade (secao 22).
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    email             text        NOT NULL,
    password_hash     text        NOT NULL,
    role              text        NOT NULL DEFAULT 'STUDENT',
    status            text        NOT NULL DEFAULT 'ACTIVE',
    email_verified_at timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    deleted_at        timestamptz,

    CONSTRAINT users_role_check   CHECK (role IN ('STUDENT', 'MODERATOR', 'ADMIN')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    -- E-mail e sempre normalizado para minusculas na aplicacao. O CHECK garante
    -- que nenhum caminho de escrita burle a normalizacao e crie duplicata logica.
    CONSTRAINT users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT users_email_format    CHECK (email LIKE '%_@_%._%')
);

CREATE UNIQUE INDEX users_email_key ON users (email);
CREATE INDEX users_status_idx ON users (status) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- profiles: preferencias do estudante. 1:1 com users, cascata na exclusao.
-- ---------------------------------------------------------------------------
CREATE TABLE profiles (
    user_id                uuid        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    display_name           text,
    locale                 text        NOT NULL DEFAULT 'pt-BR',
    timezone               text        NOT NULL DEFAULT 'America/Sao_Paulo',
    learning_goal          text,
    daily_goal_minutes     integer     NOT NULL DEFAULT 15,
    prefers_reduced_motion boolean     NOT NULL DEFAULT false,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT profiles_daily_goal_check CHECK (daily_goal_minutes BETWEEN 5 AND 240),
    CONSTRAINT profiles_display_name_len CHECK (display_name IS NULL OR char_length(display_name) BETWEEN 1 AND 60)
);

-- ---------------------------------------------------------------------------
-- refresh_tokens: sessoes com rotacao.
-- Guarda o SHA-256 do token, nunca o token em claro. Vazamento do banco nao
-- entrega sessoes ativas. `replaced_by` permite detectar reuso de token
-- revogado, que e sinal de roubo de credencial.
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  bytea       NOT NULL,
    issued_at   timestamptz NOT NULL DEFAULT now(),
    expires_at  timestamptz NOT NULL,
    revoked_at  timestamptz,
    replaced_by uuid        REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    user_agent  text,

    CONSTRAINT refresh_tokens_expiry_check CHECK (expires_at > issued_at),
    CONSTRAINT refresh_tokens_hash_len     CHECK (octet_length(token_hash) = 32)
);

CREATE UNIQUE INDEX refresh_tokens_hash_key ON refresh_tokens (token_hash);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id, expires_at DESC);
-- Suporta a varredura de limpeza de tokens expirados/revogados.
CREATE INDEX refresh_tokens_cleanup_idx ON refresh_tokens (expires_at) WHERE revoked_at IS NULL;
