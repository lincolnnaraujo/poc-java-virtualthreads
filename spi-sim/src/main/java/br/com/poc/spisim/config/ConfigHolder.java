package br.com.poc.spisim.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Guarda a configuracao corrente do simulador de forma thread-safe (AtomicReference),
 * sem uso de {@code synchronized} (constitution P2). Reconfiguravel em runtime.
 */
@Component
public class ConfigHolder {

    private final AtomicReference<SpiConfig> referencia;

    public ConfigHolder(
            @Value("${spi.latencia-media-ms:100}") long latenciaMediaMs,
            @Value("${spi.jitter-ms:30}") long jitterMs,
            @Value("${spi.taxa-falha:0.0}") double taxaFalha,
            @Value("${spi.taxa-timeout:0.0}") double taxaTimeout,
            @Value("${spi.taxa-rejeicao:0.0}") double taxaRejeicao,
            @Value("${spi.timeout-ms:3000}") long timeoutMs) {
        this.referencia = new AtomicReference<>(
                new SpiConfig(latenciaMediaMs, jitterMs, taxaFalha, taxaTimeout, taxaRejeicao, timeoutMs));
    }

    public SpiConfig atual() {
        return referencia.get();
    }

    public void atualizar(SpiConfig nova) {
        referencia.set(nova);
    }
}
