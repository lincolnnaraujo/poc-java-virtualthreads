package br.com.poc.pix.domain;

/**
 * Falha ao contatar o autorizador SPI (5xx, timeout ou indisponibilidade).
 * E a excecao retentavel (Resilience4j @Retry) e contabilizada pelo circuit breaker.
 * Apos esgotar as tentativas, propaga e leva a mensagem a DLQ (constitution P3.4 - opcao a).
 */
public class AutorizadorIndisponivelException extends RuntimeException {

    public AutorizadorIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public AutorizadorIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
