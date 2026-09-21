# Tarefas — Fatia 004: CQRS + Consulta

- [x] T4.1 — Flyway `V4__pix_read_model.sql` (`consulta_pix` + índice de status).
- [x] T4.2 — Eventos de saída: `PixAutorizado`/`PixRejeitado` (003) + **novo** `PixEmProcessamento` (para a etapa); `PixRecebido` enriquecido com nomes.
- [x] T4.3 — Fila `pix.projecao.q` + 4 bindings (recebido, em_processamento, autorizado, rejeitado); trusted packages no converter.
- [x] T4.4 — Projeção com upsert idempotente (`ON CONFLICT DO UPDATE`) + guardas anti-regressão de status (tolerante a ordem/concorrência).
- [x] T4.5 — Portas `ConsultaPixReadPort` + `ConsultaPixProjecaoPort` + adapter único `ConsultaPixJdbc`.
- [x] T4.6 — `ConsultarPixUseCase`/`ConsultarPixService` + `PixQueryController` (`GET /v1/pix/{endToEndId}`); `ProjetarPixUseCase`/`ProjecaoPixService` + `ProjecaoPixListener` (`@RabbitHandler` multi-tipo).
- [x] T4.7 — `404` problem+json (`PixNaoEncontradoException`) para inexistente; `400` para e2e malformado; OpenAPI cobre a rota.
- [~] T4.8 — Testes unitários da projeção → **delegado ao QA** (P9). Verificação comportamental feita end-to-end.
- [x] T4.9 — Validar cenários 1–3 do `spec.md` end-to-end. **✅ verificado.**

## Pronto quando
- [x] Ciclo de vida visível via `GET`; projeção idempotente; consistência eventual demonstrada; 404 correto.

## Notas de verificação (2026-09-21)
- **Build:** `mvn clean package` (Docker) OK — ArchUnit (P1) + domínio verdes.
- **Correção durante o dev:** um único pool fixo de threads era compartilhado pelos 2 containers de listener (recebido + projeção) → *"Consumer failed to start... enough threads"*. Ajustado: modo `platform` usa executor default por-container; o limite vem de `benchmark.consumer.concurrency`.
- **Cenário 1 (estado final):** `POST` → `GET` reflete `AUTORIZADO` com `valor`, `pagadorNome`, `recebedorNome` e os 3 timestamps: `recebidoEm` → `processandoEm` → `finalizadoEm`.
- **Cenário 2 (consistência eventual):** com SPI a 800ms, `GET` imediato retornou `200` com `EM_PROCESSAMENTO` (estado intermediário, não erro), progredindo para `AUTORIZADO`.
- **Cenário 3 (inexistente):** `GET` de e2e desconhecido → `404 application/problem+json`.
- **Bônus:** e2e malformado → `400`.
- **Projeção event-driven completa:** `PixRecebido`→`PixEmProcessamento`→`PixAutorizado` consumidos por `@RabbitHandler` (roteados por `__TypeId__`) atualizam o read model.
