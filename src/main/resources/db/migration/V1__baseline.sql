-- Fatia 001 (fundacao): baseline do schema.
-- Nenhuma tabela de negocio ainda:
--   * modelo de escrita normalizado (transacao_pix, pagador, recebedor) entra na V2 (fatia 002)
--   * historico de eventos (transacao_evento) entra na V3 (fatia 003)
--   * read model (consulta_pix) entra na V4 (fatia 004)
--
-- Este arquivo existe para estabelecer o versionamento Flyway desde o dia 1.

-- Schema padrao 'public' ja existe; registro de baseline sem DDL de negocio.
DO $$
BEGIN
    RAISE NOTICE 'Flyway baseline V1 aplicado - schema pronto para as fatias seguintes';
END $$;
