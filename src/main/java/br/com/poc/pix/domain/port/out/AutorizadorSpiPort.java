package br.com.poc.pix.domain.port.out;

import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.model.ResultadoAutorizacao;
import br.com.poc.pix.domain.model.Valor;

/**
 * Porta de saida para o autorizador SPI (chamada bloqueante externa).
 * E o ponto de I/O que o benchmark de virtual threads estressa (constitution P3).
 */
public interface AutorizadorSpiPort {

    /**
     * @throws br.com.poc.pix.domain.AutorizadorIndisponivelException apos esgotar as tentativas
     *         de resiliencia (timeout/5xx) ou com o circuit breaker aberto.
     */
    ResultadoAutorizacao autorizar(EndToEndId endToEndId, Valor valor);
}
