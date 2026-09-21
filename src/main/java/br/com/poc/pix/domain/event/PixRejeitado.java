package br.com.poc.pix.domain.event;

import java.time.Instant;

/** Evento de dominio: transacao Pix rejeitada pelo SPI. */
public record PixRejeitado(String endToEndId, String motivo, Instant ocorridoEm) {
}
