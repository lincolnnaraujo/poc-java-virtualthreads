# Spec — Fatia 003: Consumer Assíncrono + Resiliência

## Objetivo (WHAT)
Consumir o evento `PixRecebido`, chamar o **autorizador SPI** (porta), evoluir o estado da
transação (`EM_PROCESSAMENTO` → `AUTORIZADO`/`REJEITADO`) e tratar falhas com retry/circuit
breaker/DLQ. **Aqui mora o I/O bloqueante que a POC quer estressar** — é o palco do virtual threads.

## Justificativa (WHY)
No fluxo assíncrono, o bloqueio real (chamada SPI ~100 ms) está no consumer. A comparação
platform vs virtual threads no **executor do consumer** é o headline do benchmark (fatia 007).

## Escopo
- Listener RabbitMQ do evento `PixRecebido`.
- Chamada à `AutorizadorSpiPort` (adapter HTTP → simulador SPI da fatia 005) protegida por
  **Resilience4j**: timeout 1s, retry 3x backoff exponencial (100→200→400 ms), circuit breaker (50%).
- Evolução de estado persistida (write model) + publicação de evento de resultado (`PixAutorizado`/`PixRejeitado`).
- **Idempotência no consumo (P4):** entrega duplicada não re-processa (checagem por `endToEndId`).
- **DLQ (P3.4 = opção a):** após esgotar retries → dead-letter queue + **métrica/contador**; sem reprocessamento automático.
- Ponto de injeção do **executor plugável** (platform pool ↔ virtual-per-task) — toggle real na 007.

## Fora de escopo
- Projeção/consulta (004), o simulador SPI em si (005), dashboards (006), execução do benchmark (007).

## Critérios de Aceitação (Gherkin)

### Cenário 1: autorização bem-sucedida
- **Dado** um evento `PixRecebido` e o SPI respondendo sucesso
- **Quando** o consumer processa
- **Então** o estado evolui para `AUTORIZADO`
- **E** um evento `PixAutorizado` é publicado.

### Cenário 2: idempotência no consumo
- **Dado** um evento cujo `endToEndId` já foi processado
- **Quando** o consumer recebe a entrega duplicada
- **Então** não há re-processamento nem segunda chamada ao SPI
- **E** a mensagem é reconhecida (ACK).

### Cenário 3 (erro): SPI falha além do limite → DLQ
- **Dado** o SPI falhando/timeout acima de 3 tentativas
- **Quando** o consumer processa
- **Então** após retries com backoff a mensagem vai para a **DLQ**
- **E** o contador de DLQ é incrementado
- **E** o estado fica `EM_PROCESSAMENTO` ou `REJEITADO` (não `AUTORIZADO`).

### Cenário 4: circuit breaker protege sob falha sustentada
- **Dado** taxa de falha do SPI acima de 50% na janela
- **Quando** o breaker abre
- **Então** chamadas subsequentes falham rápido (sem esperar timeout) até half-open.

## Handoff QA
- QA cobre: idempotência sob duplicatas, comportamento de DLQ, transições de estado, breaker.
