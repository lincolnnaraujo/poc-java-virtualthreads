package br.com.poc.pix.domain.event;

import java.time.Instant;

/** Evento de dominio: transacao Pix autorizada pelo SPI. */
public record PixAutorizado(String endToEndId, Instant ocorridoEm) {
}
