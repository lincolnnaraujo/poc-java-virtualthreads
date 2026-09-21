-- Fatia 003: historico de transicoes de estado (auditoria do ciclo de vida no lado de escrita).
-- Cada mudanca de status registra uma linha; util para inspecao/apresentacao.

CREATE TABLE transacao_evento (
    id           BIGSERIAL PRIMARY KEY,
    transacao_id BIGINT       NOT NULL REFERENCES transacao_pix (id) ON DELETE CASCADE,
    status       VARCHAR(20)  NOT NULL,
    motivo       VARCHAR(200),
    ocorrido_em  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_transacao_evento_transacao ON transacao_evento (transacao_id);
