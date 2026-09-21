package br.com.poc.pix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da POC de Virtual Threads (Java 21) com API de Pix.
 *
 * <p>Fatia 001 (fundacao): apenas o esqueleto hexagonal + infraestrutura.
 * Regras de Pix, comando, consumer, consulta e benchmark entram nas fatias seguintes
 * (ver {@code specs/}).</p>
 */
@SpringBootApplication
public class PixPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(PixPocApplication.class, args);
    }
}
