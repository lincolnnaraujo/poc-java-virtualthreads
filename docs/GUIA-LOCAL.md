# Guia de Execução Local — POC Virtual Threads (Pix)

Passo a passo para **subir a stack**, **exercitar a API**, **rodar o benchmark** e **ler os gráficos**
no Grafana. Runbook operacional — copie e cole. Para o "porquê" das decisões, veja `../README.md`,
`../memory/constitution.md` e `../docs/epico.md`.

> **Shell:** os comandos `curl` abaixo assumem um shell tipo bash (no Windows, use o **Git Bash**).
> Comandos `docker` funcionam em qualquer shell. No Claude Code, você pode rodar um comando na
> sessão prefixando com `!`.

---

## 1. Pré-requisitos

| Ferramenta | Uso | Verificar |
|---|---|---|
| **Docker Desktop** + Compose v2 | sobe toda a stack | `docker compose version` |
| **JDK 21+** (opcional) | rodar o gerador de carga `benchmark/Load.java` | `java -version` |
| Portas livres | 8080, 8081, 5672, 15672, 15692, 9090, 3000 | — |

> Se o Docker não estiver rodando, abra o **Docker Desktop** e aguarde o engine ficar pronto
> (`docker info` deve responder sem erro).

---

## 2. Subir a stack

Na raiz do repositório:

```bash
docker compose up -d --build
```

Sobe 6 containers: `postgres`, `rabbitmq`, `spi-sim`, `app`, `prometheus`, `grafana`.
O `app` só inicia **depois** de postgres, rabbitmq e spi-sim ficarem saudáveis.

Acompanhe até tudo ficar `healthy`:

```bash
docker compose ps
```

Confirme o health da aplicação (deve retornar `200` com `"status":"UP"`):

```bash
curl -s http://localhost:8080/actuator/health
```

> **Primeira vez:** o build baixa dependências (alguns minutos). As próximas subidas são rápidas
> (cache de dependências no Docker).

### Endereços

| Serviço | URL | Credenciais |
|---|---|---|
| API (app) | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui/index.html | — |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs | — |
| RabbitMQ (Management) | http://localhost:15672 | guest / guest |
| Prometheus | http://localhost:9090 | — |
| **Grafana** | http://localhost:3000 | admin / admin |
| Simulador SPI | http://localhost:8081 | — |

---

## 3. Smoke test funcional (fluxo assíncrono ponta a ponta)

### 3.1 Registrar um Pix (`POST /v1/pix` → 202)

```bash
curl -i -X POST http://localhost:8080/v1/pix \
  -H 'content-type: application/json' \
  -d '{
    "endToEndId":"E1111111111111111111111111111111",
    "txid":"TESTE-001",
    "valor":150.75,
    "infoEntreClientes":"pagamento pedido 123",
    "pagador":{"nome":"Fulano de Tal","cpfCnpj":"12345678901","ispb":"12345678","agencia":"0001","conta":"1234567","tipoConta":"CACC"},
    "recebedor":{"nome":"Ciclano Souza","cpfCnpj":"98765432100","ispb":"87654321","agencia":"0002","conta":"7654321","tipoConta":"CACC","chave":"ciclano@email.com","tipoChave":"EMAIL"}
  }'
```

Esperado: **`202 Accepted`** com `status: RECEBIDO` e header `Location: /v1/pix/E1111...`.

> **Formato do `endToEndId`:** `E` + 31 caracteres alfanuméricos (32 no total).
> Valor `<= 0` ou campos faltando → **`400`** `application/problem+json`.
> Reenviar o mesmo `endToEndId` → **`200 JA_REGISTRADO`** (idempotência).

### 3.2 Consultar o ciclo de vida (`GET /v1/pix/{endToEndId}`)

```bash
curl -s http://localhost:8080/v1/pix/E1111111111111111111111111111111
```

Após ~1s (processamento assíncrono), esperado: `status: AUTORIZADO` com os timestamps
`recebidoEm` → `processandoEm` → `finalizadoEm`.

> Consultar **imediatamente** pode retornar `RECEBIDO`/`EM_PROCESSAMENTO` (consistência eventual).
> `endToEndId` inexistente → **`404`**.

### 3.3 Ver os eventos no RabbitMQ

Abra http://localhost:15672 (guest/guest) → aba **Queues**. Você verá:
- `pix.recebido.q` — fila do consumer;
- `pix.projecao.q` — fila da projeção (read model);
- `pix.recebido.dlq` — dead-letter queue.

---

## 4. Rodada de resiliência (falha do SPI → DLQ)

O simulador SPI aceita reconfiguração em runtime. Force falha total e veja a mensagem ir para a DLQ:

```bash
# 1) SPI sempre falha
curl -s -X POST http://localhost:8081/spi/config -H 'content-type: application/json' \
  -d '{"latenciaMediaMs":50,"jitterMs":0,"taxaFalha":1.0,"taxaTimeout":0.0,"taxaRejeicao":0.0,"timeoutMs":3000}'

# 2) registra um Pix
curl -s -o /dev/null -X POST http://localhost:8080/v1/pix -H 'content-type: application/json' \
  -d '{"endToEndId":"E2222222222222222222222222222222","valor":10.00,
       "pagador":{"nome":"P","cpfCnpj":"12345678901","ispb":"12345678","conta":"1","tipoConta":"CACC"},
       "recebedor":{"nome":"R","cpfCnpj":"98765432100","ispb":"87654321","conta":"2","tipoConta":"CACC","chave":"r@e.com","tipoChave":"EMAIL"}}'

# 3) após 3 retries (com backoff), a mensagem cai na DLQ. Confira o contador:
curl -s http://localhost:8080/actuator/metrics/pix_dlq_total

# 4) restaure o SPI ao normal
curl -s -X POST http://localhost:8081/spi/config -H 'content-type: application/json' \
  -d '{"latenciaMediaMs":100,"jitterMs":30,"taxaFalha":0.0,"taxaTimeout":0.0,"taxaRejeicao":0.0,"timeoutMs":3000}'
```

No RabbitMQ Management, `pix.recebido.dlq` terá 1 mensagem. O status da transação fica em
`EM_PROCESSAMENTO` (não autorizada).

---

## 5. Benchmark: platform vs virtual threads

A comparação muda **apenas** os toggles (arquivos `.env.platform` / `.env.virtual` na raiz).

### 5.1 Config A — platform threads

```bash
docker compose down -v                                   # limpa o banco
docker compose --env-file .env.platform up -d --build
curl -s http://localhost:8080/actuator/prometheus | grep -m1 benchmark_mode   # confirma "platform"
```

### 5.2 SPI como gargalo controlado (idêntico nas duas rodadas)

```bash
curl -s -X POST http://localhost:8081/spi/config -H 'content-type: application/json' \
  -d '{"latenciaMediaMs":500,"jitterMs":0,"taxaFalha":0.0,"taxaTimeout":0.0,"taxaRejeicao":0.0,"timeoutMs":3000}'
```

### 5.3 Gerar carga

**Opção A — loader Java (rápido, usado neste repo):**

```bash
java benchmark/Load.java 800 1 150 http://localhost:8080
# argumentos: <N> <startId> <concorrencia> <baseUrl>
```

**Opção B — Gatling (relatório HTML; ideal em máquina separada):**

```bash
docker run --rm --network poc-java_default -v "$PWD/benchmark":/bench -w /bench \
  -v pix_gatling_m2:/root/.m2 maven:3.9-eclipse-temurin-21 \
  mvn -q gatling:test -Dgatling.simulationClass=pix.PixPostSimulation \
      -Dbase.url=http://app:8080 -Dtarget.rps=800 -Dramp.seconds=30 -Dhold.seconds=60
# relatório: benchmark/target/gatling/
```

### 5.4 Repetir com a Config B — virtual threads

```bash
docker compose down -v
docker compose --env-file .env.virtual up -d --build
# repita 5.2 (SPI 500ms) e 5.3 (mesma carga)
```

### 5.5 O que observar

Compare os dois runs no Grafana (seção 6). O **delta limpo** neste hardware é a **economia de
threads** (`jvm_threads_live`), não o throughput absoluto. Entenda o porquê em
`benchmark/RELATORIO.md` (co-location confound). Números medidos de referência:

| | Platform | Virtual |
|---|---:|---:|
| `jvm_threads_live` (threads de plataforma) | ~280 | ~51 |
| Throughput/thread | 0,24 | ~1,08 |

---

## 6. Ler os gráficos no Grafana

1. Abra http://localhost:3000 (admin/admin).
2. Menu **Dashboards** → **"Pix Benchmark - Virtual vs Platform Threads"** (já provisionado).
3. Gere carga (seção 5) para popular os painéis. O refresh é de 5s.

### Painéis e o que cada um conta

| Painel | O que olhar |
|---|---|
| **Throughput (TPS)** | requisições/s em `/v1/pix`, por `benchmark_mode`. |
| **Latência p95/p99** | percentis de latência HTTP. |
| **Threads de plataforma vivas** | 🎯 **o painel-chave.** Sob **virtual** fica baixo mesmo com alta concorrência; sob **platform** escala com a carga. |
| **HikariCP** | conexões ativas/em espera/ociosas — mostra o pool como gargalo. |
| **Filas RabbitMQ** | profundidade de `pix.recebido.q` / `pix.projecao.q` / DLQ (backlog do consumer). |
| **DLQ total** | contador `pix_dlq_total`. |
| **Heap / CPU** | uso de memória e CPU do processo. |

> As séries carregam a label `benchmark_mode` (`platform`/`virtual`) — rode as duas configs em
> momentos diferentes e compare as curvas na mesma janela de tempo.

### Detecção de pinning (bônus)

O flag `-Djdk.tracePinnedThreads=full` está ativo. Confirme:

```bash
docker compose logs app | grep 'JAVA_TOOL_OPTIONS'
```

Como o código é VT-friendly (constitution P2), **não** deve haver eventos de pinning nos logs.

---

## 7. Verificar métricas cruas no Prometheus

Abra http://localhost:9090 → aba **Status → Targets** (os 3 jobs devem estar **UP**).
Na aba **Graph**, teste algumas queries:

```promql
sum(rate(http_server_requests_seconds_count{application="pix-app"}[1m]))
jvm_threads_live_threads{application="pix-app"}
hikaricp_connections_pending{application="pix-app"}
rabbitmq_queue_messages{queue="pix.recebido.q"}
pix_dlq_total{application="pix-app"}
```

---

## 8. Parar e limpar

```bash
docker compose down          # para e remove containers (mantém o volume do Postgres)
docker compose down -v       # idem + apaga o banco (começar do zero)
```

---

## 9. Troubleshooting

| Sintoma | Causa provável | Ação |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop parado | abra o Docker Desktop e aguarde `docker info` responder |
| `app` fica `health: starting` e depois `503` | ainda subindo (Flyway/Spring) | aguarde ~60–90s; veja `docker compose logs app` |
| `Consumer failed to start ... threads` | concorrência do consumer alta demais p/ o modo platform | reduza `BENCHMARK_CONSUMER_CONCURRENCY` no `.env.platform` |
| Porta ocupada (bind: address already in use) | outra app usa a porta | libere a porta ou ajuste o mapeamento no `docker-compose.yml` |
| Injeção lenta com `curl` em loop | overhead de processo no Windows | use `benchmark/Load.java` ou Gatling (seção 5.3) |
| Grafana sem dados | falta carga / janela de tempo | gere carga (seção 5) e ajuste o intervalo do dashboard |

---

## Referências rápidas

- Visão geral e arquitetura → `../README.md`
- Diagramas C4 (contexto/container/componente) → `./ARQUITETURA-C4.md`
- Princípios normativos → `../memory/constitution.md`
- História refinada (épico) → `./epico.md`
- Relatório do benchmark → `../benchmark/RELATORIO.md`
- Especificações por fatia → `../specs/00N-*/`
