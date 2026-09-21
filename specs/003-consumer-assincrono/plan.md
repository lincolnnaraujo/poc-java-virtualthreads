# Plano Técnico — Fatia 003: Consumer Assíncrono

## Portas
- **Out:** `AutorizadorSpiPort { ResultadoAutorizacao autorizar(TransacaoPix) }`.
- **In (evento):** `ProcessarPixUseCase { void processar(PixRecebido) }`.
- **Out:** reutiliza `PixRepositoryPort` (+ método `atualizarStatus`) e `EventPublisherPort`.

## Adapter de mensageria (consumo)
- `@RabbitListener` na fila `pix.recebido.q` (bound à `pix.exchange` / `pix.recebido`).
- **Concorrência plugável:** o processamento é despachado a um `ExecutorService` injetado.
  - Bean `pixConsumerExecutor`: por perfil/propriedade, ou `Executors.newFixedThreadPool(N)`
    (platform, baseline) ou `Executors.newVirtualThreadPerTaskExecutor()` (virtual).
  - Toggle: `benchmark.consumer.mode = platform|virtual` (default definido na 007).
- Ack manual após sucesso; nack/reject → dead-letter.

## Resiliência (Resilience4j) na porta SPI
```
resilience4j:
  timelimiter.instances.spi.timeoutDuration: 1s
  retry.instances.spi: { maxAttempts: 3, waitDuration: 100ms, exponentialBackoffMultiplier: 2 }
  circuitbreaker.instances.spi: { failureRateThreshold: 50, slidingWindowSize: 20, waitDurationInOpenState: 5s }
```
- Ordem de decorators: `CircuitBreaker` → `Retry` → `TimeLimiter` → chamada SPI.
- ⚠️ **P2:** o cliente HTTP do SPI **não** pode usar `synchronized` durante I/O (pinning).
  Usar cliente VT-friendly (ex: `java.net.http.HttpClient` ou RestClient sobre ele).

## Topologia RabbitMQ (DLQ)
- Exchange principal `pix.exchange` (topic).
- Fila `pix.recebido.q` com args:
  - `x-dead-letter-exchange: pix.dlx`
  - fila DLQ `pix.recebido.dlq` bound à `pix.dlx`.
- Após esgotar retries **da aplicação** (Resilience4j), o consumer faz `reject(requeue=false)`
  → mensagem cai na DLQ. (Evitar loop de requeue infinito.)
- Métrica: `pix_dlq_total` (Micrometer counter) — exposta para a fatia 006.

## Estado / idempotência
- `ProcessarPixService`: primeiro checa idempotência (status atual do `endToEndId`); se já
  `AUTORIZADO`/`REJEITADO`, ACK e retorna. Senão marca `EM_PROCESSAMENTO`, chama SPI, evolui.
- Transições persistidas em `transacao_pix.status` + tabela de histórico `transacao_evento`
  (para a fatia 004 projetar o ciclo de vida).

## Riscos / atenção VT
- Se o SPI (fatia 005) tiver latência, o retry síncrono segura a thread mais tempo — **bom** para
  demonstrar virtual threads. Não otimizar isso para "resolver".
- Pool do HikariCP: cada processamento que persiste precisa de conexão; sob virtual threads com
  milhares de tarefas, o pool vira backpressure natural. Tunar/observar na 006/007.
