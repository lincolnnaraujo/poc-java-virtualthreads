# Benchmark — Virtual vs Platform Threads

Aparato de carga e procedimento para comparar as duas configs. Resultados em `RELATORIO.md`.

## Toggles (a variável isolada — P3)

As duas configs diferem **apenas** nos arquivos de ambiente da raiz:
- `.env.platform` — platform threads (baseline).
- `.env.virtual` — virtual threads.

```bash
docker compose --env-file .env.platform up -d --build   # Config A
# ... roda a carga, coleta métricas no Grafana ...
docker compose down -v
docker compose --env-file .env.virtual up -d --build     # Config B
```

## Procedimento reprodutível

1. Suba uma config (`--env-file`).
2. Defina a latência do SPI (gargalo controlado, idêntico nas duas rodadas):
   ```bash
   curl -X POST http://localhost:8081/spi/config -H 'content-type: application/json' \
     -d '{"latenciaMediaMs":500,"jitterMs":0,"taxaFalha":0.0,"taxaTimeout":0.0,"taxaRejeicao":0.0,"timeoutMs":3000}'
   ```
3. Aqueça (warmup) e rode a carga (opção A ou B abaixo).
4. Colete no Grafana (`http://localhost:3000`, admin/admin): **TPS, p95/p99, `jvm_threads_live`,
   HikariCP, profundidade das filas, `pix_dlq_total`**. Filtre por `benchmark_mode`.
5. Repita para a outra config e compare.

## Opção A — Gatling (recomendado para números de HTTP / ambiente dedicado)

Roda num container Maven na rede do compose, apontando para `app:8080`:

```bash
# S1: ingestão (POST /v1/pix)
docker run --rm --network poc-java_default -v "$PWD/benchmark":/bench -w /bench \
  -v pix_gatling_m2:/root/.m2 maven:3.9-eclipse-temurin-21 \
  mvn -q gatling:test -Dgatling.simulationClass=pix.PixPostSimulation \
      -Dbase.url=http://app:8080 -Dtarget.rps=800 -Dramp.seconds=30 -Dhold.seconds=60

# S2: consulta (GET /v1/pix/{id})
docker run --rm --network poc-java_default -v "$PWD/benchmark":/bench -w /bench \
  -v pix_gatling_m2:/root/.m2 maven:3.9-eclipse-temurin-21 \
  mvn -q gatling:test -Dgatling.simulationClass=pix.PixQuerySimulation \
      -Dbase.url=http://app:8080 -Dtarget.rps=1000
```

O relatório HTML do Gatling fica em `benchmark/target/gatling/`.

> **Nota de reprodutibilidade:** para medir o **throughput** de forma justa, rode o Gatling a
> partir de **outra máquina** (não co-locada), senão o gerador de carga compete por CPU com a app.

## Opção B — Loader Java rápido (usado na medição deste repo)

Gerador single-file com virtual threads (requer JDK 21+ no host). Injeta N Pix e mede a injeção;
o drain do consumer é medido pela contagem de `AUTORIZADO` no Postgres.

```bash
# injeta 800 Pix (startId, concorrencia, base url)
java benchmark/Load.java 800 1 150 http://localhost:8080

# mede o drain:
docker compose exec -T postgres psql -U pix -d pix -t -A \
  -c "SELECT count(*) FROM transacao_pix WHERE status='AUTORIZADO';"
```

## Interpretação honesta

Neste notebook (tudo co-locado), o **delta limpo é a economia de threads** (`jvm_threads_live`),
não o throughput absoluto — ver `RELATORIO.md` para o porquê. Para o throughput, use ambiente
dedicado + Gatling em máquina separada.
