# Plano Técnico — Fatia 007: Benchmark

## Toggles (a variável isolada — P3)
| Toggle | Config A (baseline) | Config B (virtual) |
|--------|---------------------|--------------------|
| HTTP (`spring.threads.virtual.enabled`) | `false` (Tomcat pool = 200) | `true` |
| Consumer (`benchmark.consumer.mode`) | `platform` (`newFixedThreadPool(N)`) | `virtual` (`newVirtualThreadPerTaskExecutor`) |

- Aplicar via perfis Spring ou variáveis de ambiente no `docker-compose` (dois `.env`: `.env.platform`, `.env.virtual`).
- **Nada mais muda entre os runs.** Documentar o diff exato (idealmente 1–2 linhas).

## Gatling
- Projeto Gatling (Java/Scala DSL) fora do container da app (roda na máquina host).
- **S1 (comando):** feeder gera `endToEndId` únicos; `POST /v1/pix`; rampa de usuários
  (ex: `rampUsers` de 0 → 5.000 em 2 min, sustenta 3 min).
- **S2 (consulta):** consulta `endToEndId`s previamente inseridos; alta concorrência de leitura.
- Assertions: capturar p50/p95/p99, throughput, taxa de erro (< 0,1% na rodada limpa).

## Procedimento de execução (reproduzível)
1. `docker compose --env-file .env.platform up -d --build` → aquecimento → run S1 → run S2 → salvar relatório + snapshot Grafana.
2. Derrubar, subir com `.env.virtual` → repetir os mesmos runs.
3. Rodada de caos: `POST /spi/config` com `taxaFalha` alvo → repetir S1 nas duas configs.
4. Consolidar tabela comparativa (throughput, p95/p99, threads, HikariCP pending, DLQ).

## Tuning honesto (documentar tudo)
- HikariCP: definir `maximumPoolSize` e mantê-lo **igual** nas duas configs (senão vira variável extra).
  Registrar o valor e por quê (pool pequeno é backpressure — explicar na apresentação).
- JVM heap e limites de container idênticos entre configs.

## Entregáveis
- `benchmark/` com o projeto Gatling, os `.env.*`, e um `RELATORIO.md` com:
  - condições (hardware, SPI config, pool), tabela comparativa, gráficos, e a **conclusão honesta**.
- Roteiro de apresentação (bullet points) referenciando os números reais obtidos.

## Riscos / armadilhas a evitar (senão o sênior derruba)
- Co-locação do Gatling roubando CPU → manter perfil de carga idêntico nos dois runs (confusão simétrica).
- Pool do Postgres empatando as configs → é por isso que o gargalo escalável é o **SPI**, não o DB.
- Warmup insuficiente (JIT) → sempre aquecer antes de medir.
- Comparar números absolutos com nuvem → **não fazer**; a alegação é **relativa**.
