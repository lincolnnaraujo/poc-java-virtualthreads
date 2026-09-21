# Plano Técnico — Fatia 006: Observabilidade

## App (Micrometer)
- Dependência `micrometer-registry-prometheus`; expor `management.endpoints.web.exposure.include=health,prometheus,metrics`.
- Tags comuns: `app`, `benchmark_mode=platform|virtual` (via propriedade) para **filtrar no Grafana** por config.
- Métricas custom: `pix_dlq_total`, tempo de processamento do consumer, contagem por status.
- Threads: habilitar métricas de JVM (`jvm.threads.*`); para virtual threads, expor gauge custom
  contando threads virtuais ativas (via `Thread.getAllStackTraces`/JFR ou métrica dedicada).

## Prometheus
- `prometheus.yml`: jobs `app` (`app:8080/actuator/prometheus`), `spi-sim`, e opcional
  `rabbitmq` (plugin `rabbitmq_prometheus`, expõe `:15692`).
- Scrape interval 5s (resolução suficiente para a rampa; não sobrecarrega).

## Grafana (provisionado como código)
- `grafana/provisioning/datasources/prometheus.yml` (datasource).
- `grafana/provisioning/dashboards/*.json` versionados:
  - **Dashboard "Benchmark VT":** linhas comparando `benchmark_mode=platform` vs `virtual`
    para TPS, p95/p99, threads, HikariCP pending, profundidade de fila.
- Acesso `http://localhost:3000` (admin/admin em POC — documentar como não-produtivo).

## RabbitMQ
- Habilitar plugin `rabbitmq_prometheus` na imagem management (métricas nativas).

## Pinning
- Flags JVM: `-Djdk.tracePinnedThreads=full` (log de pinning).
- Alternativa/adicional: gravar **JFR** durante os runs; abrir no JDK Mission Control.
- Documentar no README do benchmark: onde ver o pinning e como o caso "com `synchronized`" difere do "limpo".

## Infra (docker-compose)
- Serviços `prometheus` e `grafana` (limites 2 vCPU / 1.5 GB juntos), volumes para dashboards.

## Riscos
- Custo da instrumentação de threads virtuais (evitar varredura cara em hot path). Preferir
  métricas do próprio runtime/JFR a `getAllStackTraces` em alta frequência.
