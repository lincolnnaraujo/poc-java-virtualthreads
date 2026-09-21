package br.com.poc.spisim.service;

/** Resultado interno de uma autorizacao: decisao + latencia efetivamente aplicada. */
public record AutorizarResultado(Decisao decisao, long latenciaMs) {
}
