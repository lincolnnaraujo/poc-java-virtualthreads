package br.com.poc.spisim.web.dto;

/** Resposta de autorizacao do SPI simulado. */
public record AutorizarResponse(String endToEndId, String resultado, long latenciaMs) {
}
