package pix;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Cenario S1: ingestao de Pix via POST /v1/pix (modelo aberto).
 *
 * <p>Parametros (via -D): base.url, target.rps, ramp.seconds, hold.seconds.
 * Ex.: mvn gatling:test -Dgatling.simulationClass=pix.PixPostSimulation
 *          -Dbase.url=http://app:8080 -Dtarget.rps=800 -Dramp.seconds=30 -Dhold.seconds=60</p>
 */
public class PixPostSimulation extends Simulation {

    private final String baseUrl = System.getProperty("base.url", "http://localhost:8080");
    private final int targetRps = Integer.getInteger("target.rps", 500);
    private final int rampSeconds = Integer.getInteger("ramp.seconds", 30);
    private final int holdSeconds = Integer.getInteger("hold.seconds", 60);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .contentTypeHeader("application/json")
            .acceptHeader("application/json");

    // Feeder infinito de endToEndId unicos (E + 31 digitos).
    private final Iterator<Map<String, Object>> feeder = Stream
            .iterate(1, i -> i + 1)
            .map(i -> {
                Map<String, Object> linha = new HashMap<>();
                linha.put("e2e", String.format("E%031d", i));
                return linha;
            })
            .iterator();

    private static final String BODY = """
            {
              "endToEndId":"#{e2e}","txid":"GAT","valor":10.00,
              "pagador":{"nome":"Pagador","cpfCnpj":"12345678901","ispb":"12345678","conta":"1234567","tipoConta":"CACC"},
              "recebedor":{"nome":"Recebedor","cpfCnpj":"98765432100","ispb":"87654321","conta":"7654321","tipoConta":"CACC","chave":"r@e.com","tipoChave":"EMAIL"}
            }
            """;

    private final ScenarioBuilder cenario = scenario("POST /v1/pix")
            .feed(feeder)
            .exec(http("registrar-pix")
                    .post("/v1/pix")
                    .body(StringBody(BODY))
                    .check(status().in(200, 202)));

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
