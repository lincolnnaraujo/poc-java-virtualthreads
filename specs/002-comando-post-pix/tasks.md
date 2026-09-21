# Tarefas — Fatia 002: Comando `POST /pix`

- [x] T2.1 — Modelar domínio: `TransacaoPix` (agregado) + VOs (`EndToEndId`, `CpfCnpj`, `ChavePix`, `Ispb`, `Valor`), `StatusPix`, `TipoConta`, `TipoChave`, `Pagador`, `Recebedor`.
- [x] T2.2 — Testes unitários do domínio (JUnit puro, sem Spring) — `DominioPixTest` (invariantes/validações). **Executado no build (verde).**
- [x] T2.3 — Portas `RegistrarPixUseCase`/`RegistrarPixCommand`/`RegistroPixResultado`, `PixRepositoryPort`, `EventPublisherPort`.
- [x] T2.4 — Use case `RegistrarPixService` (application) — idempotência + publish pós-commit.
- [x] T2.5 — Flyway `V2__pix_write_model.sql` (transacao_pix + pagador + recebedor, UNIQUE `end_to_end_id`, CHECK valor>0).
- [x] T2.6 — Adapter `PixRepositoryJdbc` (`JdbcTemplate`, insert atômico `@Transactional`) + `DuplicateKeyException` → `PixJaRegistradoException`.
- [x] T2.7 — Adapter `RabbitEventPublisher` (best-effort P6) + `RabbitConfig` (exchange + Jackson JSON converter).
- [x] T2.8 — `PixCommandController` (`POST /v1/pix`), DTO + `@Valid`, `202`+`Location` (novo) / `200` (idempotente).
- [x] T2.9 — `PixExceptionHandler` (`@RestControllerAdvice`, RFC 7807 `problem+json`) para `400`.
- [x] T2.10 — Mascaramento de PII no logging (P5) — `Mascaras` (documento/chave).
- [x] T2.11 — springdoc-openapi (`/v3/api-docs` + Swagger UI).
- [x] T2.12 — Validar cenários 1–3 do `spec.md` (curl + RabbitMQ management API + psql). **✅ verificado.**

## Pronto quando
- [x] Cenários 1–3 verdes; PII mascarada; evento visível no RabbitMQ; OpenAPI acessível.

## Notas de verificação (2026-09-21)
- **Build:** `mvn clean package` (dentro do Docker) compilou tudo e rodou **ArchUnit (P1) + `DominioPixTest`** → verde (agora com domínio real, o P1 é enforçado de verdade).
- **Cenário 1 (POST válido):** `202 Accepted`, body `RECEBIDO`; banco com `RECEBIDO` + 1 pagador + 1 recebedor; **1 evento** `PixRecebido` publicado (`{endToEndId, valor, txid, ocorridoEm}` ISO-8601).
- **Cenário 3 (duplicado):** mesmo `endToEndId` → `200 JA_REGISTRADO`; banco continua com **1** linha; **nenhum 2º evento** (idempotência P4 comprovada).
- **Cenário 2 (inválido, valor=0):** `400` `application/problem+json` com `erros.valor`; **não persistido** (0 linhas).
- **PII (P5):** log do POST exibiu `pagadorDoc=***01 recebedorChave=c***@email.com`; nenhum CPF/chave em texto claro nos logs.
- **Contrato:** `/v3/api-docs` = 200 com `/v1/pix`; Swagger UI = 200.
