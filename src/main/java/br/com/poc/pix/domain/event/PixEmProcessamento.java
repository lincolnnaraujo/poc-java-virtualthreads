package br.com.poc.pix.domain.event;

import java.time.Instant;

/** Evento de dominio: a transacao Pix entrou em processamento (etapa do ciclo de vida). */
public record PixEmProcessamento(String endToEndId, Instant ocorridoEm) {
}
