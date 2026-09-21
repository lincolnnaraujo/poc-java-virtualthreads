-- Fatia 004: read model desnormalizado do ciclo de vida (CQRS-lite).
-- Uma linha por endToEndId, atualizada pela projecao a partir dos eventos de dominio.

CREATE TABLE consulta_pix (
    end_to_end_id   VARCHAR(32) PRIMARY KEY,
    status          VARCHAR(20)  NOT NULL,
    valor           NUMERIC(15,2),
    pagador_nome    VARCHAR(140),
    recebedor_nome  VARCHAR(140),
    recebido_em     TIMESTAMPTZ,
    processando_em  TIMESTAMPTZ,
    finalizado_em   TIMESTAMPTZ,
    motivo_rejeicao VARCHAR(200),
    atualizado_em   TIMESTAMPTZ
);

-- Indice para consultas agregadas por etapa (usado nos dashboards/benchmark).
CREATE INDEX idx_consulta_pix_status ON consulta_pix (status);
