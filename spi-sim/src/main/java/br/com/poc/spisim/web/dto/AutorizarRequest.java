package br.com.poc.spisim.web.dto;

import java.math.BigDecimal;

/**
 * Requisicao de autorizacao. O simulador so precisa do endToEndId para correlacao/log;
 * o valor e aceito por realismo (nao influencia a decisao simulada).
 */
public record AutorizarRequest(String endToEndId, BigDecimal valor) {
}
