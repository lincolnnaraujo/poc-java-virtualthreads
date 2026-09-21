package br.com.poc.pix.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adiciona tags comuns a todas as metricas. A tag {@code benchmark_mode} permite comparar
 * as rodadas platform vs virtual no Grafana (fatia 006/007).
 */
@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> tagsComuns(
            @Value("${benchmark.mode:platform}") String benchmarkMode) {
        return registry -> registry.config().commonTags(
                "application", "pix-app",
                "benchmark_mode", benchmarkMode);
    }
}
