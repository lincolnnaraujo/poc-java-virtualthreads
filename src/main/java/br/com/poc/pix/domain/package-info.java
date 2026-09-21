/**
 * Nucleo de dominio (hexagono).
 *
 * <p><b>Regra normativa (constitution P1):</b> nenhuma classe deste pacote pode importar
 * Spring, JDBC, RabbitMQ ou qualquer detalhe de infraestrutura. Dependencias apontam para
 * dentro. O teste {@code ArchitectureTest} falha o build se esta regra for violada.</p>
 */
package br.com.poc.pix.domain;
