# Spec — Fatia 007: Benchmark Comparativo (Gatling)

## Objetivo (WHAT)
Executar o experimento que **prova a tese**: mesma carga, duas configs (platform vs virtual),
capturando throughput, latência (p50/p95/p99) e o comportamento das threads/pool/filas, e gerar
um **relatório comparativo** para a apresentação.

## Justificativa (WHY)
É o entregável-fim da POC. Todas as fatias anteriores existem para tornar este experimento
possível, honesto e reproduzível (`constitution.md` P3).

## Escopo
- **Dois toggles independentes:**
  - HTTP: `spring.threads.virtual.enabled=true|false`.
  - Consumer: `benchmark.consumer.mode=virtual|platform` (executor plugável da fatia 003).
- Cenários **Gatling**:
  - **S1 — Ingestão + processamento:** `POST /v1/pix` em rampa até ~5.000 conexões concorrentes.
  - **S2 — Consulta:** `GET /v1/pix/{endToEndId}` sob alta concorrência de leitura.
- **Rodadas:** warmup → rodada limpa (SPI falha 0%) → rodada de caos (SPI falha X% para exercitar retry/DLQ).
- **Método reproduzível:** mesma carga/hardware; muda só o toggle. Registrar config do SPI e recursos.
- **Relatório:** HTML do Gatling + tabela comparativa + captura dos dashboards Grafana.

## Critério de Sucesso (relativo — do épico)
> No ponto de saturação (~5.000 conexões, SPI ~100 ms): **virtual** sustenta throughput ≥ 3x e
> p99 ≥ 5x menor que **platform**; platform colapsa (in-flight travado em 200, p99 estoura).
> Faixa absoluta a **confirmar no baseline**: ~1.500–3.000 TPS (virtual, neste notebook).

## Fora de escopo
- Otimização de produção; tuning além do necessário para um comparativo justo.

## Critérios de Aceitação (Gherkin)

### Cenário 1: execução comparativa reproduzível
- **Dado** o ambiente completo no ar
- **Quando** rodo o mesmo cenário Gatling em config platform e depois virtual
- **Então** obtenho dois relatórios com throughput e percentis, sob carga idêntica.

### Cenário 2: virtual supera platform no ponto de saturação
- **Dado** a rampa até ~5.000 conexões com SPI ~100 ms
- **Quando** comparo as configs
- **Então** virtual sustenta throughput ≥ 3x e p99 ≥ 5x menor; platform satura em ~200 in-flight.

### Cenário 3 (erro/caos): resiliência sob falha não quebra a medição
- **Dado** a rodada de caos (SPI com X% de falha)
- **Quando** executo a carga
- **Então** retries/DLQ atuam, o contador de DLQ sobe, e as métricas seguem sendo coletadas sem colapso total.

## Handoff QA
- QA pode validar reprodutibilidade do procedimento; a interpretação/apresentação é do dev.
