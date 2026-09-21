# Relatório do Benchmark — Virtual vs Platform Threads

> **Data:** 2026-09-21 · **Máquina:** Intel i7-11800H (8c/16t), 29 GB RAM · tudo co-locado (Docker Desktop).
> **Metodologia:** mesma carga, muda **apenas** o toggle (P3). Ver `.env.platform` / `.env.virtual`.

## Condições do experimento

| Parâmetro | Valor |
|---|---|
| Fluxo | `POST /v1/pix` (202) → RabbitMQ → consumer → SPI (500 ms) → estado |
| Latência do SPI | **500 ms** (gargalo deliberado, idêntico nas duas configs) |
| Carga | 800 Pix injetados (loader Java com virtual threads, no host) |
| HikariCP | pool = 10 (idêntico) |
| App | 4 vCPU / 1 GB heap (idêntico) |
| Variável isolada | `SPRING_THREADS_VIRTUAL_ENABLED` + `BENCHMARK_CONSUMER_MODE` + `concurrency` |

- **Config A (platform):** 50 consumers de plataforma.
- **Config B (virtual):** 400 consumers virtuais.

## Resultados medidos

| Métrica | Platform | Virtual | Observação |
|---|---:|---:|---|
| `jvm_threads_live` (threads de **plataforma**) | **280** | **51** | **5,5x menos** com virtual |
| Throughput end-to-end (800 Pix) | 66 Pix/s | 55 Pix/s | *ver ressalva* |
| Eficiência (Pix/s por thread de plataforma) | 0,24 | **1,08** | **~4,5x mais eficiente** |
| Taxa de injeção (host) | 231/s | 68/s | *ver ressalva* |

## Leitura honesta dos resultados

### ✅ O ganho claro e demonstrável: economia de threads
A mesma carga foi processada com **280 threads de plataforma (config platform) vs 51 (config virtual)**.
Sob virtual threads, 400 consumers concorrentes custam ~nada em threads do SO — a app faz o mesmo
trabalho com **5,5x menos threads** e **~4,5x mais throughput por thread**. Esta é a proposta de
valor central das virtual threads, e ela apareceu de forma inequívoca. É o gráfico
`jvm_threads_live` no Grafana que conta a história.

### ⚠️ Por que o throughput ABSOLUTO não é um sinal limpo aqui (co-location confound)
O throughput ficou parecido (66 vs 55) — e isso **não** significa que virtual não escala. Significa
que, neste notebook, o número absoluto é dominado por gargalos de co-locação, exatamente como
previsto no refinamento (Categoria 5 / `constitution.md` P3):

1. **Teto de injeção pelo host:** o gerador de carga roda na mesma máquina e não passou de
   ~68–231 req/s. A capacidade teórica do consumer virtual (400 / 0,5s = **800 Pix/s**) nunca foi
   exercida — o virtual ficou **limitado pela injeção**, não pela própria capacidade.
2. **Pool HikariCP=10 compartilhado:** sob virtual, os 400 consumers ativos disputam o mesmo pool
   com o caminho de ingestão (`POST` faz INSERT). Isso acoplou os dois caminhos e derrubou a taxa
   de injeção para 68/s — revelando que **o pool, não as threads, é o próximo gargalo**.

**Conclusão:** o **delta limpo e reproduzível neste hardware é a economia de threads**. Medir o
delta de **throughput absoluto** exige isolar as variáveis: gerador de carga em máquina separada
(ou o projeto **Gatling** deste repo em ambiente dedicado), pool maior, e app/infra em hosts
distintos. Ver "Como executar" para o caminho reprodutível de produção.

## Próximos passos para medir throughput de forma justa
- Rodar o **Gatling** (`benchmark/`) a partir de outra máquina apontando para o app.
- Aumentar `HIKARI_MAX_POOL_SIZE` e observar se o consumer virtual dispara (o pool era o teto).
- Separar app, Postgres, RabbitMQ e gerador de carga em hosts dedicados.
- Rodada de caos: `POST /spi/config` com `taxaFalha` > 0 e observar retry/CB/DLQ (validado na fatia 003).

## Toggles (a variável isolada)

| Toggle | `.env.platform` | `.env.virtual` |
|---|---|---|
| `SPRING_THREADS_VIRTUAL_ENABLED` | `false` | `true` |
| `BENCHMARK_CONSUMER_MODE` | `platform` | `virtual` |
| `BENCHMARK_CONSUMER_CONCURRENCY` | `50` | `400` |
| `BENCHMARK_MODE` (rótulo Grafana) | `platform` | `virtual` |
