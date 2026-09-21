/**
 * Adapters de saida/entrada de mensageria (RabbitMQ). Implementam {@code EventPublisherPort} e
 * hospedam os listeners de consumo. A tecnologia fica atras da porta (troca por SQS = trocar
 * adapter, sem tocar no dominio). Previstos: publisher (002), consumer (003), projecao (004).
 */
package br.com.poc.pix.adapter.out.messaging;
