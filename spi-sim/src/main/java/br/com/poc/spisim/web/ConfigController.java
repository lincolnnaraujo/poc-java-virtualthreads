package br.com.poc.spisim.web;

import br.com.poc.spisim.config.ConfigHolder;
import br.com.poc.spisim.config.SpiConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta e reconfiguracao (em runtime, sem restart) do comportamento do simulador SPI.
 */
@RestController
@RequestMapping("/spi/config")
public class ConfigController {

    private final ConfigHolder configHolder;

    public ConfigController(ConfigHolder configHolder) {
        this.configHolder = configHolder;
    }

    @GetMapping
    public SpiConfig atual() {
        return configHolder.atual();
    }

    @PostMapping
    public SpiConfig atualizar(@RequestBody SpiConfig nova) {
        configHolder.atualizar(nova);
        return configHolder.atual();
    }
}
