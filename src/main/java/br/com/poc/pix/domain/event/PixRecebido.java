package br.com.poc.pix.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de dominio: um Pix foi recebido e persistido no estado RECEBIDO.
 * Publicado (best-effort - constitution P6) para processamento assincrono (fatia 003).
 *
 * <p>Payload propositalmente enxuto e em tipos simples para serializacao JSON estavel
 * entre publisher e consumer.</p>
 */
public record PixRecebido(
        String endToEndId,
        BigDecimal valor,
        String txid,
        String pagadorNome,
        String recebedorNome,
        Instant ocorridoEm) {
}
