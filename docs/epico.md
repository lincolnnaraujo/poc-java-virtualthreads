# [ÉPICO] - POC: Virtual Threads (Java 21) sob alto TPS com API de Pix

> Documento-mãe do épico. As fatias verticais detalhadas estão em `specs/001..007`.
> Princípios normativos em `memory/constitution.md`.

## Contexto / Valor de Negócio
- **Por quê:** o time precisa de uma decisão embasada — e não "por ouvir falar" — sobre adotar
  Virtual Threads (Java 21) em serviços de alto throughput com I/O bloqueante.
- **Impacto esperado:** material de referência + benchmark reproduzível que quantifique o ganho
  (throughput e latência) de virtual threads vs platform threads sob a mesma carga.
- **Stakeholders:** desenvolvedores **sêniores** do time (audiência crítica — vão questionar a
  metodologia), Tech Lead, e futuros serviços candidatos a migrar para virtual threads.
- **Métrica-alvo:** ver "Critério de Sucesso" nos RNF. Em resumo: demonstrar ponto de ruptura
  onde platform threads colapsa e virtual threads sustenta.

## Descrição
- **Estado atual:** greenfield (repositório vazio).
- **Estado desejado:** aplicação Spring Boot 21, hexagonal, simulando fluxo Pix **assíncrono**
  (CQRS-lite), com toda a infra em Docker Compose e aparato de carga (Gatling) + observabilidade
  (Prometheus/Grafana) para executar e apresentar o benchmark comparativo.
- **Abordagem técnica:** Ports & Adapters; comando `POST /pix` publica evento; consumer
  assíncrono chama um autorizador SPI simulado (latência configurável) e evolui o estado;
  projeção CQRS atualiza uma base de leitura consultável via `GET`.

### Diagrama do fluxo (simplificado)
> Diagramas **C4** completos (Contexto → Container → Componente) em [`ARQUITETURA-C4.md`](ARQUITETURA-C4.md).
```
                 (sync, ~ms)                         (async)
Cliente ──POST /pix──▶ [Adapter REST]                RabbitMQ
                           │                          ▲   │
                           ▼                          │   ▼
                    RegistrarPixUseCase ──publish──────┘  [Consumer]
                           │                                  │
                           ▼ (JDBC)                           ▼ AutorizadorSpiPort (Resilience4j)
                    Postgres (write model                     │  → Simulador SPI (latência/falha)
                     normalizado, UNIQUE endToEndId)          ▼
                                                        atualiza estado + publica evento
Cliente ──GET /pix/{e2e}──▶ [Adapter REST] ──▶ Read Model ◀── Projeção (event handler)
                                             (desnormalizado, ciclo de vida)
```

## Decisões de Arquitetura
- **Ports & Adapters:**
  - Inbound: `RegistrarPixUseCase` (dir. por `PixCommandController`), `ConsultarPixUseCase` (dir. por `PixQueryController`).
  - Outbound: `PixRepositoryPort` (Postgres/JdbcTemplate), `EventPublisherPort` / `EventConsumerPort` (RabbitMQ), `AutorizadorSpiPort` (HTTP → simulador SPI).
- **Bounded Context:** `Pagamento Pix`. Agregado raiz: `TransacaoPix` (identidade = `endToEndId`).
- **Comunicação:** entrada síncrona (`202 Accepted`) + processamento **assíncrono** via RabbitMQ; consulta síncrona sobre read model (**consistência eventual**).
- **Resiliência:** Resilience4j na porta SPI (timeout 1s, retry 3x backoff exp., circuit breaker 50%); DLQ (descarte + métrica); idempotência por `endToEndId`.
- **Débito declarado:** dual-write best-effort (sem outbox) — ver `constitution.md` P6.

## Critérios de Aceitação (Gherkin — nível épico)

### Cenário 1: Registro de Pix bem-sucedido (happy path assíncrono)
- **Dado** um payload de Pix válido e completo
- **Quando** o cliente chama `POST /pix`
- **Então** recebe `202 Accepted` com o `endToEndId`
- **E** ao consultar `GET /pix/{endToEndId}` após o processamento, o status é `AUTORIZADO`.

### Cenário 2: Idempotência sob entrega duplicada
- **Dado** um evento de Pix já processado com determinado `endToEndId`
- **Quando** o consumer recebe uma **segunda** entrega do mesmo evento (at-least-once)
- **Então** a transação **não** é processada novamente
- **E** não há débito/efeito colateral duplicado, e a mensagem é reconhecida (ACK).

### Cenário 3: Falha do autorizador SPI (cenário de erro)
- **Dado** o simulador SPI configurado para falhar/timeout acima do limite de retries
- **Quando** o consumer processa o evento
- **Então** após 3 tentativas com backoff a mensagem é enviada à **DLQ**
- **E** um contador/métrica de DLQ é incrementado
- **E** o `GET /pix/{endToEndId}` reflete estado não-autorizado (ex: `EM_PROCESSAMENTO`/`REJEITADO`).

### Cenário 4: Demonstração do ganho de Virtual Threads (benchmark)
- **Dado** o mesmo cenário de carga aplicado em duas configs (platform vs virtual)
- **Quando** a rampa atinge ~5.000 conexões concorrentes com SPI a ~100 ms
- **Então** a config **virtual** sustenta throughput ≥ 3x e p99 ≥ 5x menor que a **platform**
- **E** a config platform colapsa (in-flight travado em 200, p99 estoura).

## Definition of Ready (DoR)
- [ ] Épico decomposto em fatias verticais (001–007). ✅
- [ ] Critérios de aceitação testáveis definidos. ✅
- [ ] Dependências de infra identificadas (Docker Compose). ✅
- [ ] Abordagem técnica revisada (esta doc + constitution). ✅
- [ ] RNF definidos (tabela abaixo). ✅
- [ ] Estimativa por fatia (Story Points) — **pendente com o time**.
- [ ] Sem impedimentos bloqueantes. ✅

## Definition of Done (DoD)
- [ ] Todas as 7 fatias com DoD próprio atendido.
- [ ] `docker-compose up` sobe app + Postgres + RabbitMQ + SPI + Prometheus + Grafana.
- [ ] Benchmark comparativo executável via Gatling com toggles documentados.
- [ ] Relatório comparativo (platform vs virtual) gerado e interpretado.
- [ ] Dashboards Grafana funcionando (TPS, p50/p95/p99, threads, HikariCP, fila RabbitMQ).
- [ ] Princípios P1–P9 (`constitution.md`) respeitados.
- [ ] Documentação direta e atualizada (repo compartilhado).
- [ ] **Handoff de testes para o QA** documentado (ver nota abaixo).

## Cenários de Teste
> ⚠️ **A estratégia formal de testes é responsabilidade do QA do time (ver `constitution.md` P9).**
> Devs entregam o domínio testável em isolamento + ambiente `docker-compose` reproduzível.
> Abaixo, os **fluxos que o QA deve cobrir** (não a implementação dos testes):

### Testes Funcionais (para o QA)
- [ ] Happy path assíncrono: `POST` → processamento → `GET` = `AUTORIZADO`.
- [ ] Validação de payload (campos obrigatórios, CPF/CNPJ, valor > 0).

### Testes de Erro / Edge Cases (para o QA)
- [ ] Idempotência (evento duplicado não re-processa).
- [ ] Falha SPI → retries → DLQ.
- [ ] Consulta antes do processamento (estado intermediário / consistência eventual).

### Testes de Integração (para o QA)
- [ ] Fluxo ponta a ponta com Postgres + RabbitMQ reais (Testcontainers) e SPI (WireMock).
- [ ] Contrato da API (OpenAPI) request/response.

## Requisitos Não-Funcionais
| Aspecto         | Requisito                                                                                  |
|-----------------|--------------------------------------------------------------------------------------------|
| Performance     | **Medido (ver `benchmark/RELATORIO.md`):** o delta LIMPO neste notebook é a **economia de threads** — mesma carga com **280 (platform) vs 51 (virtual)** threads de plataforma (5,5x menos; ~4,5x throughput/thread). O throughput **absoluto** (66 vs 55 Pix/s) é confundido por co-locação (teto de injeção do host + pool HikariCP=10 compartilhado) — medi-lo de forma justa exige ambiente dedicado + Gatling em máquina separada. |
| Segurança       | Sem auth (fora de escopo, declarado). **PII (CPF/CNPJ/chave) mascarada em logs.**          |
| Escalabilidade  | Alvo conceitual: >10M req/dia (~115 TPS médio, picos ~5x). Foco real: concorrência de milhares de conexões bloqueadas em I/O. |
| Disponibilidade | POC — sem SLA de uptime. Resiliência via Resilience4j + DLQ no fluxo assíncrono.            |
| Observabilidade | Micrometer + Prometheus + Grafana. Métricas: TPS, p50/p95/p99, threads (carrier/virtual), HikariCP (uso/espera), profundidade de fila RabbitMQ, CPU/heap. Taxa de erro < 0,1% nos runs limpos. |

## Dependências
- [ ] Docker + Docker Compose na máquina da demo.
- [ ] JDK 21 (para build local e para o Gatling).
- [ ] Recursos de máquina conforme envelope (`constitution.md`).

## Notas Adicionais
- **Limitações conhecidas:** números absolutos limitados pela co-locação (Gatling + app na mesma máquina) — comparação relativa permanece válida.
- **Débito técnico:** dual-write best-effort (sem outbox); sem autenticação.
- **Melhorias futuras:** Transactional Outbox; adapter SQS (via porta); deploy em ambiente dedicado para números absolutos de nuvem.
