-- Extensão que adiciona o tipo "vector" e a busca por similaridade ao PostgreSQL
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE usuario (
    id          UUID PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    senha_hash  VARCHAR(100) NOT NULL,
    papel       VARCHAR(20)  NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE documento (
    id                  UUID PRIMARY KEY,
    titulo              VARCHAR(200) NOT NULL,
    nome_arquivo        VARCHAR(255) NOT NULL,
    tipo                VARCHAR(20)  NOT NULL,
    conteudo            BYTEA        NOT NULL,
    hash_conteudo       VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256: impede enviar o mesmo arquivo duas vezes
    status              VARCHAR(20)  NOT NULL,
    mensagem_erro       VARCHAR(500),
    quantidade_trechos  INTEGER,
    enviado_por         UUID         NOT NULL REFERENCES usuario (id),
    criado_em           TIMESTAMPTZ  NOT NULL,
    processado_em       TIMESTAMPTZ,
    versao              BIGINT       NOT NULL DEFAULT 0  -- trava otimista (@Version)
);

CREATE INDEX idx_documento_status ON documento (status);

CREATE TABLE trecho (
    id            BIGSERIAL PRIMARY KEY,
    documento_id  UUID    NOT NULL REFERENCES documento (id) ON DELETE CASCADE,
    indice        INTEGER NOT NULL,
    conteudo      TEXT    NOT NULL,
    embedding     vector(768) NOT NULL,
    CONSTRAINT uk_trecho_documento_indice UNIQUE (documento_id, indice)
);

-- Índice HNSW: busca aproximada de vizinhos mais próximos pela distância de cosseno
CREATE INDEX idx_trecho_embedding ON trecho USING hnsw (embedding vector_cosine_ops);
