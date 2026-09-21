import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerador de carga single-file para o benchmark (JDK 21+): dispara N POSTs /v1/pix
 * concorrentes usando virtual threads no host (sem spawn de processo por request).
 *
 * <p>Uso: {@code java benchmark/Load.java <N> [startId] [concorrencia] [baseUrl]}</p>
 * <p>Ex.:  {@code java benchmark/Load.java 800 1 150 http://localhost:8080}</p>
 */
public class Load {
    public static void main(String[] a) throws Exception {
        int n = Integer.parseInt(a[0]);
        int startId = a.length > 1 ? Integer.parseInt(a[1]) : 1;
        int conc = a.length > 2 ? Integer.parseInt(a[2]) : 500;
        String base = a.length > 3 ? a[3] : "http://localhost:8080";

        HttpClient client = HttpClient.newHttpClient();
        Semaphore sem = new Semaphore(conc);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();

        long t0 = System.nanoTime();
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int k = 0; k < n; k++) {
                final int id = startId + k;
                sem.acquire();
                exec.submit(() -> {
                    try {
                        String e2e = String.format("E%031d", id);
                        String body = "{\"endToEndId\":\"" + e2e + "\",\"valor\":1.00,"
                            + "\"pagador\":{\"nome\":\"P\",\"cpfCnpj\":\"12345678901\",\"ispb\":\"12345678\",\"conta\":\"1\",\"tipoConta\":\"CACC\"},"
                            + "\"recebedor\":{\"nome\":\"R\",\"cpfCnpj\":\"98765432100\",\"ispb\":\"87654321\",\"conta\":\"2\",\"tipoConta\":\"CACC\",\"chave\":\"r@e.com\",\"tipoChave\":\"EMAIL\"}}";
                        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/v1/pix"))
                            .header("content-type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                        HttpResponse<Void> r = client.send(req, HttpResponse.BodyHandlers.discarding());
                        if (r.statusCode() < 300) ok.incrementAndGet(); else err.incrementAndGet();
                    } catch (Exception e) {
                        err.incrementAndGet();
                    } finally {
                        sem.release();
                    }
                });
            }
        }
        double sec = (System.nanoTime() - t0) / 1e9;
        System.out.printf("injetados=%d ok=%d err=%d inject_tempo=%.2fs taxa_injecao=%.0f/s%n",
            n, ok.get(), err.get(), sec, n / sec);
    }
}
