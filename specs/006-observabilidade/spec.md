# Spec — Fatia 006: Observabilidade

## Objetivo (WHAT)
Instrumentar a app e subir **Prometheus + Grafana** no `docker-compose`, com dashboards que
provem o benchmark ao vivo: TPS, latência (p50/p95/p99), **threads (carrier vs virtual)**,
HikariCP (uso/espera), profundidade de fila RabbitMQ (consumer lag), CPU/heap e DLQ.
Inclui detecção de **pinning** (P/ constitution P2 e objetivo 5.3 = demonstrar).

## Justificativa (WHY)
Audiência sênior não acredita em alegação sem gráfico. Observabilidade é o que transforma o
benchmark de "confie em mim" em "olhe os números". Sem isso, não há apresentação.

## Escopo
- Micrometer + `micrometer-registry-prometheus`; `/actuator/prometheus` exposto.
- Prometheus com scrape da app, do `spi-sim` e (se viável) métricas do RabbitMQ.
- Grafana provisionado (datasource + dashboards versionados no repo).
- Métricas-chave (abaixo) + painel comparativo platform vs virtual.
- **Pinning:** rodar com `-Djdk.tracePinnedThreads=full` e/ou evento JFR; documentar como capturar/ler.

## Métricas-chave (obrigatórias)
- HTTP: throughput (req/s), latência p50/p95/p99, taxa de erro.
- Threads: nº de virtual threads ativas, nº de carrier threads (`jdk.virtualThread*` / pool ForkJoin), platform threads no modo baseline.
- Pool: `hikaricp.connections.active/idle/pending`, tempo de espera por conexão.
- Mensageria: profundidade de `pix.recebido.q`, `pix.projecao.q`, DLQ; contador `pix_dlq_total`.
- Recursos: CPU, heap, GC.

## Fora de escopo
- Execução do benchmark em si (007) — aqui é só a instrumentação/dashboards.

## Critérios de Aceitação (Gherkin)

### Cenário 1: métricas expostas e coletadas
- **Dado** a app rodando
- **Quando** o Prometheus faz scrape
- **Então** métricas de HTTP, threads, HikariCP e filas aparecem em `http://localhost:9090`.

### Cenário 2: dashboard comparativo funcional
- **Dado** o Grafana provisionado
- **Quando** abro o dashboard do benchmark
- **Então** vejo TPS, p50/p95/p99, threads, HikariCP e fila num único painel.

### Cenário 3: detecção de pinning
- **Dado** a app com `-Djdk.tracePinnedThreads=full`
- **Quando** induzo um trecho com `synchronized` em I/O
- **Então** o evento de pinning é registrado e visível (log/JFR), demonstrável na apresentação.

## Handoff QA
- QA valida presença das métricas; a análise/interpretação é do dev/apresentador.
