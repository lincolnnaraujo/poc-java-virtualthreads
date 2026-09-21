# Tarefas — Fatia 006: Observabilidade

- [x] T6.1 — `micrometer-registry-prometheus` + `/actuator/prometheus` exposto.
- [x] T6.2 — Tag comum `benchmark_mode` (via `MetricsConfig`/`MeterRegistryCustomizer`, env `BENCHMARK_MODE`).
- [x] T6.3 — Métricas custom já existentes: `pix_dlq_total` (003), `spi_autorizacao_total`/`spi_latencia_aplicada_ms` (005).
- [~] T6.4 — Threads: usado `jvm_threads_live_threads` (threads de PLATAFORMA) como proxy — é a métrica que **não** explode sob virtual threads. Contagem exata de virtual threads ativas exigiria JFR (fora do escopo); documentado.
- [x] T6.5 — `infra/prometheus/prometheus.yml` (jobs app, spi-sim, rabbitmq) + serviço `prometheus` no compose.
- [x] T6.6 — `rabbitmq_prometheus` habilitado (`infra/rabbitmq/enabled_plugins`) + porta 15692; scrape em `/metrics/per-object` (métricas por fila).
- [x] T6.7 — Grafana provisionado: datasource `prometheus` + dashboard `pix-benchmark.json` (8 painéis).
- [x] T6.8 — Flag `-Djdk.tracePinnedThreads=full` no `JAVA_TOOL_OPTIONS` do app.
- [~] T6.9 — Demonstração de pinning (`synchronized` em I/O): a INFRA está pronta (flag ativo); o caso induzido é atividade da rodada de caos da fatia 007.
- [x] T6.10 — Validar cenários 1–3 do `spec.md`. **✅ verificado.**
- [ ] T6.11 — Documentar no README como abrir Grafana e ler cada painel. *(pendente)*

## Pronto quando
- [x] Dashboard comparativo funcional; métricas de threads/HikariCP/fila/DLQ visíveis; infra de pinning pronta.

## Notas de verificação (2026-09-21)
- **Build:** `mvn clean package` (Docker) OK — ArchUnit (P1) + domínio verdes.
- **App:** `/actuator/prometheus` expõe métricas com tag `benchmark_mode="platform"`.
- **Prometheus:** 3 targets **UP** (pix-app, spi-sim, rabbitmq). Queries validadas:
  - `sum(rate(http_server_requests_seconds_count{application="pix-app"}[1m]))` → 0.38
  - `jvm_threads_live_threads` → 76 · `hikaricp_connections_active` → 0 · `pix_dlq_total` → 0
  - `rabbitmq_queue_messages{queue="pix.recebido.q"}` (via `/metrics/per-object`) → série presente
- **Ajuste durante o dev:** o `/metrics` padrão do plugin só agrega; troquei o `metrics_path` do job rabbitmq para `/metrics/per-object` para obter profundidade por fila.
- **Grafana:** health `database: ok` (v11.1.0); datasource `Prometheus` provisionado; dashboard `pix-benchmark` (HTTP 200); query via proxy do Grafana → 200.
- **Pinning:** flag ativo (`Picked up JAVA_TOOL_OPTIONS: ... -Djdk.tracePinnedThreads=full`); nenhum evento de pinning (a única linha "pinned" é o nome do flag).
