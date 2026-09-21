package br.com.poc.spisim.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Falha simulada do autorizador (responde HTTP 500 para exercitar retry/circuit breaker do cliente). */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FalhaSpiException extends RuntimeException {
    public FalhaSpiException() {
        super("Falha simulada do autorizador SPI");
    }
}
