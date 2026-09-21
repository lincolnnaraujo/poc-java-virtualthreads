-- Fatia 002: modelo de escrita normalizado do Pix (tabelas distintas).
-- Idempotencia garantida pela UNIQUE constraint em end_to_end_id (constitution P4).

CREATE TABLE transacao_pix (
    id                  BIGSERIAL PRIMARY KEY,
    end_to_end_id       VARCHAR(32)  NOT NULL,
    txid                VARCHAR(35),
    valor               NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    status              VARCHAR(20)  NOT NULL,
    info_entre_clientes VARCHAR(140),
    criado_em           TIMESTAMPTZ  NOT NULL,
    atualizado_em       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_transacao_pix_e2e UNIQUE (end_to_end_id)
);

CREATE INDEX idx_transacao_pix_status ON transacao_pix (status);

CREATE TABLE pagador (
    id           BIGSERIAL PRIMARY KEY,
    transacao_id BIGINT       NOT NULL REFERENCES transacao_pix (id) ON DELETE CASCADE,
    nome         VARCHAR(140) NOT NULL,
    cpf_cnpj     VARCHAR(14)  NOT NULL,
    ispb         VARCHAR(8)   NOT NULL,
    agencia      VARCHAR(4),
    conta        VARCHAR(20)  NOT NULL,
    tipo_conta   VARCHAR(4)   NOT NULL
);

CREATE INDEX idx_pagador_transacao ON pagador (transacao_id);

CREATE TABLE recebedor (
    id           BIGSERIAL PRIMARY KEY,
    transacao_id BIGINT       NOT NULL REFERENCES transacao_pix (id) ON DELETE CASCADE,
    nome         VARCHAR(140) NOT NULL,
    cpf_cnpj     VARCHAR(14)  NOT NULL,
    ispb         VARCHAR(8)   NOT NULL,
    agencia      VARCHAR(4),
    conta        VARCHAR(20)  NOT NULL,
    tipo_conta   VARCHAR(4)   NOT NULL,
    chave        VARCHAR(77)  NOT NULL,
    tipo_chave   VARCHAR(10)  NOT NULL
);

CREATE INDEX idx_recebedor_transacao ON recebedor (transacao_id);
