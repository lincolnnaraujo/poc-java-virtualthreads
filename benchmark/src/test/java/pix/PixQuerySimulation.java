package pix;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Cenario S2: consulta do ciclo de vida via GET /v1/pix/{endToEndId} sob alta concorrencia
 * de leitura. Consulta endToEndIds no intervalo previamente populado pelo S1.
 *
 * <p>Parametros (via -D): base.url, target.rps, ramp.seconds, hold.seconds, max.id.</p>
 */
public class PixQuerySimulation extends Simulation {

    private final String baseUrl = System.getProperty("base.url", "http://localhost:8080");
    private final int targetRps = Integer.getInteger("target.rps", 1000);
    private final int rampSeconds = Integer.getInteger("ramp.seconds", 20);
    private final int holdSeconds = Integer.getInteger("hold.seconds", 40);
    private final int maxId = Integer.getInteger("max.id", 5000);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json");

    private final Iterator<Map<String, Object>> feeder = Stream
            .generate(() -> {
                Map<String, Object> linha = new HashMap<>();
                linha.put("e2e", String.format("E%031d", ThreadLocalRandom.current().nextInt(1, maxId + 1)));
                return linha;
            })
            .iterator();

    private final ScenarioBuilder cenario = scenario("GET /v1/pix/{id}")
            .feed(feeder)
            .exec(http("consultar-pix")
                    .get("/v1/pix/#{e2e}")
                    // 200 (encontrado) ou 404 (ainda nao projetado) sao respostas validas.
                    .check(status().in(200, 404)));

    {
        setUp(
                cenario.injectOpen(
                        rampUsersPerSec(1).to(targetRps).during(rampSeconds),
                        constantUsersPerSec(targetRps).during(holdSeconds)
                )
        ).protocols(httpProtocol)
         .assertions(global().failedRequests().percent().lt(1.0));
    }
}
