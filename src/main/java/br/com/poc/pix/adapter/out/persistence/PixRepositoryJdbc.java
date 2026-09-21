package br.com.poc.pix.adapter.out.persistence;

import br.com.poc.pix.domain.PixJaRegistradoException;
import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.model.Pagador;
import br.com.poc.pix.domain.model.Recebedor;
import br.com.poc.pix.domain.model.StatusPix;
import br.com.poc.pix.domain.model.TransacaoPix;
import br.com.poc.pix.domain.port.out.PixRepositoryPort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Adapter de persistencia (JdbcTemplate, sem JPA - constitution P7).
 * Grava o modelo de escrita normalizado (transacao_pix + pagador + recebedor) atomicamente.
 */
@Repository
public class PixRepositoryJdbc implements PixRepositoryPort {

    private final JdbcTemplate jdbc;

    public PixRepositoryJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existe(EndToEndId endToEndId) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(1) FROM transacao_pix WHERE end_to_end_id = ?",
                Integer.class, endToEndId.valor());
        return total != null && total > 0;
    }

    @Override
    @Transactional
    public void salvar(TransacaoPix transacao) {
        try {
            Long transacaoId = jdbc.queryForObject(
                    "INSERT INTO transacao_pix "
                            + "(end_to_end_id, txid, valor, status, info_entre_clientes, criado_em, atualizado_em) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class,
                    transacao.endToEndId().valor(),
                    transacao.txid(),
                    transacao.valor().quantia(),
                    transacao.status().name(),
                    transacao.infoEntreClientes(),
                    OffsetDateTime.ofInstant(transacao.criadoEm(), ZoneOffset.UTC),
                    OffsetDateTime.ofInstant(transacao.atualizadoEm(), ZoneOffset.UTC));

            inserirPagador(transacaoId, transacao.pagador());
            inserirRecebedor(transacaoId, transacao.recebedor());
        } catch (DuplicateKeyException duplicado) {
            // UNIQUE(end_to_end_id) violado: idempotencia (constitution P4).
            throw new PixJaRegistradoException(transacao.endToEndId().valor());
        }
    }

    @Override
    public Optional<StatusPix> statusAtual(EndToEndId endToEndId) {
        List<String> encontrados = jdbc.query(
                "SELECT status FROM transacao_pix WHERE end_to_end_id = ?",
                (rs, linha) -> rs.getString(1), endToEndId.valor());
        return encontrados.isEmpty() ? Optional.empty() : Optional.of(StatusPix.valueOf(encontrados.get(0)));
    }

    @Override
    public void atualizarStatus(EndToEndId endToEndId, StatusPix novoStatus) {
        atualizarStatus(endToEndId, novoStatus, null);
    }

    @Override
    @Transactional
    public void atualizarStatus(EndToEndId endToEndId, StatusPix novoStatus, String motivo) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        Long transacaoId = jdbc.queryForObject(
                "UPDATE transacao_pix SET status = ?, atualizado_em = ? WHERE end_to_end_id = ? RETURNING id",
                Long.class, novoStatus.name(), agora, endToEndId.valor());

        jdbc.update(
                "INSERT INTO transacao_evento (transacao_id, status, motivo, ocorrido_em) VALUES (?, ?, ?, ?)",
                transacaoId, novoStatus.name(), motivo, agora);
    }

    private void inserirPagador(Long transacaoId, Pagador pagador) {
        jdbc.update(
                "INSERT INTO pagador (transacao_id, nome, cpf_cnpj, ispb, agencia, conta, tipo_conta) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                transacaoId,
                pagador.nome(),
                pagador.cpfCnpj().valor(),
                pagador.ispb().valor(),
                pagador.agencia(),
                pagador.conta(),
                pagador.tipoConta().name());
    }

    private void inserirRecebedor(Long transacaoId, Recebedor recebedor) {
        jdbc.update(
                "INSERT INTO recebedor (transacao_id, nome, cpf_cnpj, ispb, agencia, conta, tipo_conta, chave, tipo_chave) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                transacaoId,
                recebedor.nome(),
                recebedor.cpfCnpj().valor(),
                recebedor.ispb().valor(),
                recebedor.agencia(),
                recebedor.conta(),
                recebedor.tipoConta().name(),
                recebedor.chave().valor(),
                recebedor.tipoChave().name());
    }
}
