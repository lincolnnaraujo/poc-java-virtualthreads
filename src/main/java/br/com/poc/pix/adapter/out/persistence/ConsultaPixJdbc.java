package br.com.poc.pix.adapter.out.persistence;

import br.com.poc.pix.domain.event.PixAutorizado;
import br.com.poc.pix.domain.event.PixEmProcessamento;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.event.PixRejeitado;
import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.port.out.ConsultaPixProjecaoPort;
import br.com.poc.pix.domain.port.out.ConsultaPixReadPort;
import br.com.poc.pix.domain.port.out.ConsultaPixView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adapter JDBC do read model (CQRS): leitura da consulta + upserts idempotentes da projecao.
 *
 * <p>Os upserts sao tolerantes a ordem (multiplos consumers na fila de projecao): guardas
 * impedem regressao de status e cada etapa grava seu proprio timestamp.</p>
 */
@Repository
public class ConsultaPixJdbc implements ConsultaPixReadPort, ConsultaPixProjecaoPort {

    private final JdbcTemplate jdbc;

    public ConsultaPixJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ===== Leitura =====

    @Override
    public Optional<ConsultaPixView> porEndToEndId(EndToEndId endToEndId) {
        List<ConsultaPixView> encontrados = jdbc.query(
                "SELECT end_to_end_id, status, valor, pagador_nome, recebedor_nome, "
                        + "recebido_em, processando_em, finalizado_em, motivo_rejeicao, atualizado_em "
                        + "FROM consulta_pix WHERE end_to_end_id = ?",
                (rs, linha) -> new ConsultaPixView(
                        rs.getString("end_to_end_id"),
                        rs.getString("status"),
                        rs.getBigDecimal("valor"),
                        rs.getString("pagador_nome"),
                        rs.getString("recebedor_nome"),
                        paraInstant(rs.getTimestamp("recebido_em")),
                        paraInstant(rs.getTimestamp("processando_em")),
                        paraInstant(rs.getTimestamp("finalizado_em")),
                        rs.getString("motivo_rejeicao"),
                        paraInstant(rs.getTimestamp("atualizado_em"))),
                endToEndId.valor());
        return encontrados.stream().findFirst();
    }

    // ===== Projecao (escrita do read model) =====

    @Override
    public void aoReceber(PixRecebido evento) {
        jdbc.update("""
                INSERT INTO consulta_pix
                    (end_to_end_id, status, valor, pagador_nome, recebedor_nome, recebido_em, atualizado_em)
                VALUES (?, 'RECEBIDO', ?, ?, ?, ?, ?)
                ON CONFLICT (end_to_end_id) DO UPDATE SET
                    valor = EXCLUDED.valor,
                    pagador_nome = EXCLUDED.pagador_nome,
                    recebedor_nome = EXCLUDED.recebedor_nome,
                    recebido_em = EXCLUDED.recebido_em,
                    atualizado_em = EXCLUDED.atualizado_em
                """,
                evento.endToEndId(), evento.valor(), evento.pagadorNome(), evento.recebedorNome(),
                paraOffset(evento.ocorridoEm()), paraOffset(Instant.now()));
    }

    @Override
    public void aoProcessar(PixEmProcessamento evento) {
        jdbc.update("""
                INSERT INTO consulta_pix (end_to_end_id, status, processando_em, atualizado_em)
                VALUES (?, 'EM_PROCESSAMENTO', ?, ?)
                ON CONFLICT (end_to_end_id) DO UPDATE SET
                    status = CASE WHEN consulta_pix.status = 'RECEBIDO'
                                  THEN 'EM_PROCESSAMENTO' ELSE consulta_pix.status END,
                    processando_em = EXCLUDED.processando_em,
                    atualizado_em = EXCLUDED.atualizado_em
                """,
                evento.endToEndId(), paraOffset(evento.ocorridoEm()), paraOffset(Instant.now()));
    }

    @Override
    public void aoAutorizar(PixAutorizado evento) {
        jdbc.update("""
                INSERT INTO consulta_pix (end_to_end_id, status, finalizado_em, atualizado_em)
                VALUES (?, 'AUTORIZADO', ?, ?)
                ON CONFLICT (end_to_end_id) DO UPDATE SET
                    status = 'AUTORIZADO',
                    finalizado_em = EXCLUDED.finalizado_em,
                    atualizado_em = EXCLUDED.atualizado_em
                """,
                evento.endToEndId(), paraOffset(evento.ocorridoEm()), paraOffset(Instant.now()));
    }

    @Override
    public void aoRejeitar(PixRejeitado evento) {
        jdbc.update("""
                INSERT INTO consulta_pix (end_to_end_id, status, finalizado_em, motivo_rejeicao, atualizado_em)
                VALUES (?, 'REJEITADO', ?, ?, ?)
                ON CONFLICT (end_to_end_id) DO UPDATE SET
                    status = 'REJEITADO',
                    finalizado_em = EXCLUDED.finalizado_em,
                    motivo_rejeicao = EXCLUDED.motivo_rejeicao,
                    atualizado_em = EXCLUDED.atualizado_em
                """,
                evento.endToEndId(), paraOffset(evento.ocorridoEm()), evento.motivo(), paraOffset(Instant.now()));
    }

    private static OffsetDateTime paraOffset(Instant instante) {
        return instante == null ? null : OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static Instant paraInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
