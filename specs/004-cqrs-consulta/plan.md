# Plano Técnico — Fatia 004: CQRS + Consulta

## Read model (Flyway `V4__pix_read_model.sql`)
```
consulta_pix(
  end_to_end_id   VARCHAR PK,
  status          VARCHAR,          -- etapa atual
  valor           NUMERIC(15,2),
  pagador_nome    VARCHAR,          -- desnormalizado (PII mascarada só em log, não na tabela)
  recebedor_nome  VARCHAR,
  recebido_em     TIMESTAMPTZ,
  processando_em  TIMESTAMPTZ,
  finalizado_em   TIMESTAMPTZ,
  motivo_rejeicao VARCHAR NULL,
  atualizado_em   TIMESTAMPTZ
)
-- índice em status para consultas agregadas do benchmark
```

## Projeção (event handlers)
- `@RabbitListener` em fila própria `pix.projecao.q` bound aos eventos de domínio
  (`pix.recebido`, `pix.autorizado`, `pix.rejeitado`).
- Handler faz **upsert** idempotente (`INSERT ... ON CONFLICT (end_to_end_id) DO UPDATE`).
- Cada evento seta a coluna de timestamp correspondente e avança o `status`.
- Idempotência: aplicar só transições válidas (não regride status).

## Portas e camadas
- **In:** `ConsultarPixUseCase { ConsultaPixView consultar(EndToEndId) }`.
- **Out:** `ConsultaPixReadPort { Optional<ConsultaPixView> porEndToEndId(EndToEndId) }`.
- Adapter leitura: `ConsultaPixReadJdbc` (JdbcTemplate, `SELECT` simples).
- Adapter web: `PixQueryController` → `GET /v1/pix/{endToEndId}`; `404` via advice.

## Decisão CQRS
- Mesma instância Postgres, **tabelas separadas** (write vs read). Não é banco separado — é
  CQRS-lite, suficiente para a POC e honesto na apresentação (explicar o trade-off).

## Atenção VT / benchmark
- O `GET` é o segundo eixo de carga: sob alta concorrência de leitura, a camada HTTP com virtual
  threads volta a importar. Manter a query leve (single-row por PK) para isolar o efeito de threads.
