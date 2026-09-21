package br.com.poc.spisim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simulador do autorizador SPI do Pix (fatia 005).
 *
 * <p>Serve como o I/O bloqueante CONTROLADO do benchmark: latencia e taxas de falha/timeout
 * configuraveis em runtime. Roda com virtual threads habilitado para nao virar o gargalo
 * do experimento (constitution P3).</p>
 */
@SpringBootApplication
public class SpiSimApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpiSimApplication.class, args);
    }
}
