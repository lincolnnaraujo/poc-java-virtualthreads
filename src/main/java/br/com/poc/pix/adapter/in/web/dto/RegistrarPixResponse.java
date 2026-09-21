package br.com.poc.pix.adapter.in.web.dto;

/**
 * Resposta do POST /v1/pix.
 */
public record RegistrarPixResponse(String endToEndId, String status, String mensagem) {
}
