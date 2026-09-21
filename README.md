# POC — Virtual Threads (Java 21) com API de Pix

> **O que é:** POC de **estudo e demonstração** de **Virtual Threads (Java 21)** sob alto TPS,
> simulando uma API de **Pix** com arquitetura **hexagonal** e fluxo **assíncrono (CQRS-lite)**.
> **Para quê:** benchmark **honesto e reproduzível** — platform threads vs virtual threads — para
> apresentação a um time técnico de desenvolvedores sêniores.
>
> **Este repositório é compartilhado.** A documentação é intencionalmente **direta**. Leia a
> `memory/constitution.md` antes de contribuir — ela é normativa.

---

## 📚 Documentação

A documentação vive em `docs/`, `memory/`, `benchmark/` e `specs/`. Por onde começar:

| Preciso... | Documento |
|---|---|
| **Rodar e validar localmente** — subir, testar, benchmark, ler gráficos | 👉 [`docs/GUIA-LOCAL.md`](docs/GUIA-LOCAL.md) |
| Entender a **arquitetura** — diagramas **C4** (contexto → container → componente) | [`docs/ARQUITETURA-C4.md`](docs/ARQUITETURA-C4.md) |
| Ver a **história refinada** do épico — contexto, decisões, critérios de aceite | [`docs/epico.md`](docs/epico.md) |
| **Contribuir** — princípios não-negociáveis (fronteiras hexagonais, VT-friendly, PII…) | [`memory/constitution.md`](memory/constitution.md) |
| Ver o **resultado do benchmark** — números reais + conclusão honesta | [`benchmark/RELATORIO.md`](benchmark/RELATORIO.md) |
| **Executar o benchmark** — procedimento (Gatling / loader Java) | [`benchmark/README.md`](benchmark/README.md) |
| Detalhe **por fatia** — spec (o quê/porquê), plan (o como), tasks (checklist) | [`specs/`](specs/) |

---

## Stack

| Camada          | Tecnologia                                              |
|-----------------|---------------------------------------------------------|
| Linguagem       | Java 21 (LTS — Virtual Threads GA)                       |
| Framework       | Spring Boot (Web MVC — servlet **bloqueante**)          |
| Build           | Maven                                                    |
| Arquitetura     | Hexagonal (Ports & Adapters) + CQRS-lite                 |
| Persistência    | PostgreSQL via **JDBC puro (`JdbcTemplate`)** — sem JPA  |
| Mensageria      | RabbitMQ (ver rationale abaixo)                          |
| Resiliência     | Resilience4j (timeout, retry, circuit breaker) + DLQ    |
| Observabilidade | Micrometer + Prometheus + Grafana                        |
| Carga           | Gatling                                                  |
| Infra           | Docker Compose (tudo containerizado)                     |

## Arquitetura (visão rápida)

```
POST /pix (202) ─▶ RegistrarPixUseCase ─▶ Postgres (write, UNIQUE endToEndId)
                          │
                          └─publish─▶ RabbitMQ ─▶ Consumer ─▶ AutorizadorSpiPort ─▶ Simulador SPI
                                                     │            (Resilience4j)     (latência/falha)
                                                     └─▶ atualiza estado + projeta Read Model
GET /pix/{endToEndId} ─▶ ConsultarPixUseCase ─▶ Read Model (ciclo de vida, consistência eventual)
```

Diagramas **C4** (Contexto → Container → Componente): [`docs/ARQUITETURA-C4.md`](docs/ARQUITETURA-C4.md).
Detalhes e decisões: `docs/epico.md` (seção "Decisões de Arquitetura").

## Estrutura do repositório (Spec-Driven Development)

```
docs/GUIA-LOCAL.md          # runbook: subir, testar, benchmark, ler gráficos (COMECE AQUI p/ rodar)
memory/constitution.md      # princípios normativos (LEIA PRIMEIRO p/ contribuir)
docs/epico.md               # história refinada nível épico
benchmark/RELATORIO.md      # resultado do benchmark + conclusão honesta
specs/00N-*/spec.md         # o QUÊ e PORQUÊ de cada fatia
specs/00N-*/plan.md         # o COMO (design técnico) de cada fatia
specs/00N-*/tasks.md        # tarefas acionáveis (checklist) de cada fatia
```

### Fatias verticais (ordem de implementação sugerida)
1. **001-fundacao** — scaffold hexagonal + docker-compose (Postgres, RabbitMQ) + Maven.
2. **002-comando-post-pix** — `POST /pix`, domínio, JDBC, idempotência, publish (best-effort).
3. **003-consumer-assincrono** — consumer, SPI + Resilience4j, DLQ, evolução de estado.
4. **004-cqrs-consulta** — read model + `GET /pix/{endToEndId}` (ciclo de vida).
5. **005-simulador-spi** — autorizador SPI simulado com latência/falha configuráveis.
6. **006-observabilidade** — Micrometer/Prometheus/Grafana + dashboards.
7. **007-benchmark** — toggles platform/virtual + Gatling + relatório comparativo.

## Como rodar

> 📖 **Passo a passo completo (subir, testar, benchmark e ler os gráficos): [`docs/GUIA-LOCAL.md`](docs/GUIA-LOCAL.md).**

```bash
# subir toda a infra + app
docker compose up -d --build

# app:        http://localhost:8080   (Swagger: /swagger-ui/index.html)
# grafana:    http://localhost:3000   (admin/admin — dashboards do benchmark)
# rabbitmq:   http://localhost:15672  (guest/guest — management UI)
# prometheus: http://localhost:9090
# spi-sim:    http://localhost:8081   (autorizador simulado)
```

## Benchmark

Compara **duas configs mudando apenas os toggles** (`.env.platform` vs `.env.virtual`):
camada HTTP (`spring.threads.virtual.enabled`) + executor do consumer (`benchmark.consumer.mode`).

- **Procedimento reprodutível** → [`benchmark/README.md`](benchmark/README.md)
- **Resultado medido + conclusão honesta** → [`benchmark/RELATORIO.md`](benchmark/RELATORIO.md)

> **Resultado (resumo):** o delta **limpo** neste hardware é a **economia de threads** — mesma
> carga com **~280 (platform) vs ~51 (virtual)** threads de plataforma. O throughput **absoluto**
> é confundido por co-locação (gerador de carga na mesma máquina + pool compartilhado), como
> previsto em `memory/constitution.md` (P3). Medir throughput de forma justa exige ambiente
> dedicado + Gatling em máquina separada.

## Rationale: por que RabbitMQ (e não SQS/Kafka)

Decisão registrada para **não se perder** com o tempo:

- **RabbitMQ (escolhido):** 100% local, rápido, **determinístico**, DLQ nativa — **não contamina o benchmark**.
- **SQS (AWS) rejeitado para o benchmark:** LocalStack não é representativo de performance; SQS FIFO tem teto ~300 TPS; AWS real adiciona latência de rede como variável confundidora.
- **Kafka rejeitado:** overkill; semântica de partição vira ruído na apresentação.

**Salvaguarda:** a mensageria fica **atrás de uma porta** (`EventPublisherPort`/`EventConsumerPort`).
Trocar RabbitMQ por **SQS** no futuro = trocar o adapter, **sem tocar no domínio**.

## Handoff QA

A **estratégia formal de testes é responsabilidade do QA do time** (`constitution.md` P9).
Os devs entregam:
1. **Domínio testável em isolamento** (sem Spring/JDBC/RabbitMQ).
2. **Ambiente `docker-compose` reproduzível** para o QA exercitar contrato → integração.

Os fluxos que o QA deve cobrir estão listados em `docs/epico.md` → seção "Cenários de Teste".

## Débitos técnicos declarados
- **Dual-write best-effort** (sem Transactional Outbox) — risco conhecido e aceito para a POC.
- **Sem autenticação/autorização** — fora de escopo declarado.
- **PII sempre mascarada em logs** (isto **não** é débito — é requisito, `constitution.md` P5).
