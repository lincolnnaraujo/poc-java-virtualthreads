# Tarefas — Fatia 007: Benchmark

- [x] T7.1 — Toggle do consumer (`benchmark.consumer.mode` platform|virtual) fechado na fatia 003/004.
- [x] T7.2 — `.env.platform` e `.env.virtual` (única diferença = os 4 toggles); compose parametrizado.
- [x] T7.3 — Projeto Gatling (`benchmark/`) com cenário S1 (`PixPostSimulation`, feeder de endToEndId únicos, modelo aberto configurável).
- [x] T7.4 — Cenário S2 (`PixQuerySimulation`, `GET /v1/pix/{id}` sob alta concorrência).
- [x] T7.5 — HikariCP `maximum-pool-size=10` idêntico nas duas configs (documentado — virou o gargalo revelado).
- [x] T7.6 — Roteiro de execução (warmup → platform → virtual) no `benchmark/README.md`; SPI 500ms idêntico.
- [~] T7.7 — Rodada de caos: retry/CB/DLQ **validados na fatia 003**; script de `POST /spi/config` documentado no README.
- [x] T7.8 — `benchmark/RELATORIO.md` consolidado (condições + tabela + conclusão honesta).
- [~] T7.9 — Critério de sucesso: **reavaliado**. O delta LIMPO neste hardware é a **economia de threads (5,5x)**; o throughput absoluto é confundido por co-locação (documentado, era o risco previsto na Cat. 5).
- [x] T7.10 — Número real medido e registrado no `RELATORIO.md` (platform 66 vs virtual 55 Pix/s; 280 vs 51 threads).
- [~] T7.11 — Roteiro de apresentação: os pontos-chave estão no `RELATORIO.md` (economia de threads + confounds honestos).

## Pronto quando
- [x] Duas configs executadas sob carga idêntica; números reais coletados; `RELATORIO.md` pronto.
- [~] O gerador de carga Java (`benchmark/Load.java`) + o projeto Gatling estão prontos; medição de throughput absoluto justo exige ambiente dedicado (documentado).

## Notas de verificação (2026-09-21)
- **Descoberta metodológica:** injeção via `bash+curl` no Windows/Git Bash é inviável (spawn de processo por request → ~11 Pix/s). Trocado por **loader Java single-file com virtual threads** no host (`benchmark/Load.java`) — é o motivo de existir o Gatling.
- **Config platform (50 consumers, SPI 500ms):** 800 Pix → **66 Pix/s**, **`jvm_threads_live`=280**.
- **Config virtual (400 consumers, SPI 500ms):** 800 Pix → **55 Pix/s** (limitado pela injeção do host), **`jvm_threads_live`=51**.
- **Resultado limpo:** mesma carga → **5,5x menos threads de plataforma** com virtual; **~4,5x mais throughput por thread**.
- **Confound documentado (Cat. 5 / P3):** throughput absoluto dominado pelo teto de injeção do host + pool HikariCP=10 compartilhado (ingestão vs consumer). Medir throughput justo exige gerador de carga em máquina separada + pool maior.
