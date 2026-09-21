# Tarefas — Fatia 003: Consumer Assíncrono

- [x] T3.1 — Porta `AutorizadorSpiPort` + `ResultadoAutorizacao` + `AutorizadorIndisponivelException`.
- [x] T3.2 — `ProcessarPixUseCase`/`ProcessarPixService` (idempotência → EM_PROCESSAMENTO → SPI → estado final).
- [x] T3.3 — Flyway `V3__pix_evento_historico.sql` (`transacao_evento`) + transições registradas.
- [x] T3.4 — Adapter `AutorizadorSpiClient` com `java.net.http.HttpClient` (VT-friendly, sem `synchronized` em I/O — P2).
- [x] T3.5 — Resilience4j: timeout 1s (via HttpRequest), `@Retry` (3x backoff exp.), `@CircuitBreaker` (50%). *(Timeout no client, não `@TimeLimiter` — mais direto p/ chamada bloqueante; desvio documentado.)*
- [x] T3.6 — `RabbitConfig`: exchange, fila `pix.recebido.q` (com `x-dead-letter-exchange`), DLX `pix.dlx`, DLQ `pix.recebido.dlq`.
- [x] T3.7 — `@RabbitListener` + `RabbitListenerConfig` com executor plugável (`benchmark.consumer.mode` platform|virtual).
- [x] T3.8 — `defaultRequeueRejected=false` + `AmqpRejectAndDontRequeueException` → DLQ; contador `pix_dlq_total`.
- [x] T3.9 — Publicar `PixAutorizado`/`PixRejeitado` via `EventPublisherPort`.
- [~] T3.10 — Testes unitários do `ProcessarPixService` → **delegado ao QA** (constitution P9). Verificação comportamental feita end-to-end.
- [x] T3.11 — Validar cenários 1–4 do `spec.md` com o simulador SPI (005). **✅ verificado.**

## Pronto quando
- [x] Cenários 1–3 verdes; DLQ recebendo mensagens; `pix_dlq_total` incrementando; idempotência no consumo comprovada.
- [~] Cenário 4 (CB): breaker ativo e contabilizando chamadas; abertura plena sob falha sustentada será exercida na rodada de caos da fatia 007.

## Notas de verificação (2026-09-21)
- **Build:** `mvn clean package` (Docker) OK — ArchUnit (P1) + domínio verdes.
  - *(1 correção durante o dev: factory `ResultadoAutorizacao.autorizado()` colidia com o accessor do record → renomeado para `aprovada()`/`rejeitada()`.)*
- **Cenário 1 (happy path):** `POST` `202` → consumer → SPI → status `AUTORIZADO`; `transacao_evento`: `EM_PROCESSAMENTO`→`AUTORIZADO`; evento `PixAutorizado` publicado.
- **Cenário 2 (idempotência):** reenvio do mesmo `PixRecebido` (publish direto na exchange) → **não** reprocessou (2 transições antes/depois), status intacto, DLQ zerada.
- **Cenário 3 (DLQ):** SPI com `taxaFalha=1.0` → após 3 retries a mensagem foi para `pix.recebido.dlq` (1 msg), `pix_dlq_total=1`, status preso em `EM_PROCESSAMENTO`.
- **Cenário 4 (CB):** `resilience4j.circuitbreaker.calls{name=spi}` registrou as chamadas — retry + circuit breaker ativos.
- **Executor plugável:** `benchmark.consumer.mode` (platform|virtual) e `benchmark.consumer.concurrency` prontos para a fatia 007.
