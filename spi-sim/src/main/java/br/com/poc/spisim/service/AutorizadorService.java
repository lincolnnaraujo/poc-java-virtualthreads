package br.com.poc.spisim.service;

import br.com.poc.spisim.config.ConfigHolder;
import br.com.poc.spisim.config.SpiConfig;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Nucleo do simulador: aplica latencia (com jitter) e sorteia o desfecho conforme as taxas
 * configuradas (timeout, falha, rejeicao, ou autorizado). Metricas expostas via Micrometer.
 *
 * <p>O bloqueio e feito com {@link Thread#sleep} — barato numa virtual thread — o que e
 * didaticamente adequado para a POC (constitution P2/P3).</p>
 */
@Component
public class AutorizadorService {

    private final ConfigHolder configHolder;
    private final MeterRegistry registry;
    private final DistributionSummary latenciaAplicada;

    public AutorizadorService(ConfigHolder configHolder, MeterRegistry registry) {
        this.configHolder = configHolder;
        this.registry = registry;
        this.latenciaAplicada = DistributionSummary.builder("spi_latencia_aplicada_ms")
                .description("Latencia aplicada pelo simulador SPI em milissegundos")
                .baseUnit("ms")
                .register(registry);
    }

    public AutorizarResultado autorizar(String endToEndId) {
        SpiConfig cfg = configHolder.atual();
        double sorteio = ThreadLocalRandom.current().nextDouble();

        double limiteTimeout = cfg.taxaTimeout();
        double limiteFalha = limiteTimeout + cfg.taxaFalha();
        double limiteRejeicao = limiteFalha + cfg.taxaRejeicao();

        // Caso timeout: dorme alem do timeout do cliente e ainda responde.
        if (sorteio < limiteTimeout) {
            dormir(cfg.timeoutMs());
            latenciaAplicada.record(cfg.timeoutMs());
            contar("timeout");
            return new AutorizarResultado(Decisao.AUTORIZADO, cfg.timeoutMs());
        }

        long latencia = calcularLatencia(cfg);
        dormir(latencia);
        latenciaAplicada.record(latencia);

        if (sorteio < limiteFalha) {
            contar("falha");
            throw new FalhaSpiException();
        }
        if (sorteio < limiteRejeicao) {
            contar("rejeitado");
            return new AutorizarResultado(Decisao.REJEITADO, latencia);
        }
        contar("autorizado");
        return new AutorizarResultado(Decisao.AUTORIZADO, latencia);
    }

    private long calcularLatencia(SpiConfig cfg) {
        if (cfg.jitterMs() == 0) {
            return cfg.latenciaMediaMs();
        }
        long delta = ThreadLocalRandom.current().nextLong(-cfg.jitterMs(), cfg.jitterMs() + 1);
        return Math.max(0, cfg.latenciaMediaMs() + delta);
    }

    private void contar(String resultado) {
        registry.counter("spi_autorizacao_total", "resultado", resultado).increment();
    }

    private void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrompido durante simulacao de latencia", e);
        }
    }
}
