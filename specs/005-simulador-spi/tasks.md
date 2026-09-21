# Tarefas — Fatia 005: Simulador SPI

- [x] T5.1 — Serviço Spring Boot minimalista `spi-sim/` (Java 21, `spring.threads.virtual.enabled=true`, porta 8081).
- [x] T5.2 — Endpoint `POST /spi/autorizar` (latência + sorteio de desfecho).
- [x] T5.3 — Config dinâmica `GET/POST /spi/config` (`ConfigHolder` com `AtomicReference`, sem `synchronized` — P2).
- [x] T5.4 — Latência com jitter + taxas de falha (5xx), timeout e rejeição, com validação de faixas.
- [x] T5.5 — Métricas Micrometer/Prometheus: `spi_autorizacao_total{resultado}` + `spi_latencia_aplicada_ms`.
- [x] T5.6 — Dockerfile (cache `.m2`) + serviço `spi-sim` no `docker-compose` (limites 2 vCPU / 512 MB).
- [~] T5.7 — URL `AUTORIZADOR_SPI_URL=http://spi-sim:8081/spi/autorizar` já cabeada no serviço `app`; o `AutorizadorSpiClient` que a consome é da **fatia 003**.
- [x] T5.8 — Validar cenários 1–3 do `spec.md`. **✅ verificado.**

## Pronto quando
- [x] Simulador responde com latência/falha configuráveis e reconfiguráveis em runtime.
- [~] Integração com o consumer acontece na fatia 003 (URL já provisionada).

## Notas de verificação (2026-09-21)
- **Build:** `docker compose build spi-sim` OK (reusou cache `.m2` do app — `mvn package` ~10s).
- **Config inicial:** `GET /spi/config` → `{latenciaMediaMs:100, jitterMs:30, taxaFalha:0.0, ...}`.
- **Cenário 1 (latência+sucesso):** `POST /spi/autorizar` → `AUTORIZADO`, latência aplicada ~124–129 ms (100 ± jitter), HTTP 200.
- **Cenário 3 (reconfig runtime):** `POST /spi/config` (latência→20, taxaFalha→0.3) refletido no `GET` **sem restart**.
- **Cenário 2 (taxa de falha):** 40 chamadas com taxaFalha=0.3 → 16 respostas `500` (40% — dentro da margem estatística para n=40).
- **Métricas:** `/actuator/prometheus` expôs `spi_autorizacao_total{resultado="autorizado"}` e `{resultado="falha"}`.
