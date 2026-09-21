package br.com.poc.pix.domain.model;

import br.com.poc.pix.domain.PixInvalidoException;

import java.time.Instant;

/**
 * Agregado raiz do bounded context "Pagamento Pix". Identidade = {@link EndToEndId}.
 *
 * <p>Nesta fatia (002) so ha a criacao no estado {@link StatusPix#RECEBIDO} via
 * {@link #receber}. As transicoes de estado entram na fatia 003.</p>
 */
public class TransacaoPix {

    private final EndToEndId endToEndId;
    private final String txid;
    private final Valor valor;
    private final Pagador pagador;
    private final Recebedor recebedor;
    private StatusPix status;
    private final String infoEntreClientes;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    private TransacaoPix(EndToEndId endToEndId, String txid, Valor valor, Pagador pagador,
                         Recebedor recebedor, StatusPix status, String infoEntreClientes,
                         Instant criadoEm, Instant atualizadoEm) {
        if (endToEndId == null || valor == null || pagador == null || recebedor == null || status == null) {
            throw new PixInvalidoException("transacao Pix incompleta");
        }
        this.endToEndId = endToEndId;
        this.txid = txid;
        this.valor = valor;
        this.pagador = pagador;
        this.recebedor = recebedor;
        this.status = status;
        this.infoEntreClientes = infoEntreClientes;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    /** Cria uma nova transacao no estado inicial RECEBIDO. */
    public static TransacaoPix receber(EndToEndId endToEndId, String txid, Valor valor,
                                       Pagador pagador, Recebedor recebedor, String infoEntreClientes) {
        Instant agora = Instant.now();
        return new TransacaoPix(endToEndId, txid, valor, pagador, recebedor,
                StatusPix.RECEBIDO, infoEntreClientes, agora, agora);
    }

    public EndToEndId endToEndId() {
        return endToEndId;
    }

    public String txid() {
        return txid;
    }

    public Valor valor() {
        return valor;
    }

    public Pagador pagador() {
        return pagador;
    }

    public Recebedor recebedor() {
        return recebedor;
    }

    public StatusPix status() {
        return status;
    }

    public String infoEntreClientes() {
        return infoEntreClientes;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Instant atualizadoEm() {
        return atualizadoEm;
    }
}
