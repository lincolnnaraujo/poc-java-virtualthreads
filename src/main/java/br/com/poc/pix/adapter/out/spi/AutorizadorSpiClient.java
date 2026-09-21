package br.com.poc.pix.adapter.out.spi;

import br.com.poc.pix.domain.AutorizadorIndisponivelException;
import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.model.ResultadoAutorizacao;
import br.com.poc.pix.domain.model.Valor;
import br.com.poc.pix.domain.port.out.AutorizadorSpiPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Adapter de saida HTTP para o autorizador SPI simulado.
 *
 * <p>Resiliencia (constitution P3): timeout de 1s por chamada (via {@link HttpRequest}),
 * {@code @Retry} (3 tentativas, backoff exponencial) e {@code @CircuitBreaker} (abre em 50%).
 * Usa {@link HttpClient} (sem {@code synchronized} em I/O) para ser virtual-thread-friendly (P2).</p>
 */
@Component
public class AutorizadorSpiClient implements AutorizadorSpiPort {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI endpoint;

    public AutorizadorSpiClient(@Value("${autorizador.spi.url}") String url, ObjectMapper objectMapper) {
        this.endpoint = URI.create(url);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @CircuitBreaker(name = "spi")
    @Retry(name = "spi")
    @Override
    public ResultadoAutorizacao autorizar(EndToEndId endToEndId, Valor valor) {
        try {
            String corpo = objectMapper.writeValueAsString(Map.of(
                    "endToEndId", endToEndId.valor(),
                    "valor", valor.quantia()));

            HttpRequest requisicao = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(1))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo))
                    .build();

            HttpResponse<String> resposta = httpClient.send(requisicao, HttpResponse.BodyHandlers.ofString());

            if (resposta.statusCode() >= 500) {
                throw new AutorizadorIndisponivelException("SPI respondeu HTTP " + resposta.statusCode());
            }

            String resultado = objectMapper.readTree(resposta.body()).path("resultado").asText();
            if ("AUTORIZADO".equals(resultado)) {
                return ResultadoAutorizacao.aprovada();
            }
            return ResultadoAutorizacao.rejeitada("SPI retornou " + resultado);

        } catch (IOException e) {
            // Inclui HttpTimeoutException (timeout de 1s) -> retentavel.
            throw new AutorizadorIndisponivelException("Falha/timeout ao chamar o SPI: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AutorizadorIndisponivelException("Interrompido ao chamar o SPI", e);
        }
    }
}
