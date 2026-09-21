# Plano Técnico — Fatia 002: Comando `POST /pix`

## Domínio (sem framework — P1)
- `TransacaoPix` (agregado): `endToEndId` (VO), `txid`, `valor` (`Money`/`BigDecimal` > 0),
  `Pagador`, `Recebedor`, `StatusPix` (enum), `infoEntreClientes`.
- VOs: `EndToEndId`, `CpfCnpj` (com validação), `ChavePix`, `Ispb`.
- Regra de negócio: valida invariantes na construção (valor > 0, participantes completos).
- `StatusPix`: `RECEBIDO` (inicial nesta fatia).

## Portas
- **In:** `RegistrarPixUseCase { RegistroResultado registrar(RegistrarPixCommand) }`.
- **Out:** `PixRepositoryPort { void salvar(TransacaoPix); boolean existe(EndToEndId) }`,
  `EventPublisherPort { void publicar(PixRecebido) }`.

## Adapter de entrada (web)
- `PixCommandController` → `POST /pix`.
- `RegistrarPixRequest` (DTO) com `jakarta.validation` (`@NotNull`, `@Positive`, `@Pattern` p/ ISPB etc.).
- `@RestControllerAdvice` → `400` com corpo padronizado (RFC 7807 `application/problem+json`).
- **Log com PII mascarada** (P5): mascarar `cpfCnpj`, `chave` antes de logar.

## Adapter de persistência (JdbcTemplate)
- Tabelas (Flyway `V2__pix_write_model.sql`):
  - `transacao_pix(id PK, end_to_end_id UNIQUE NOT NULL, txid, valor NUMERIC(15,2), status, info_entre_clientes, criado_em, atualizado_em)`
  - `pagador(id PK, transacao_id FK, nome, cpf_cnpj, ispb, agencia, conta, tipo_conta)`
  - `recebedor(id PK, transacao_id FK, nome, cpf_cnpj, ispb, agencia, conta, tipo_conta, chave, tipo_chave)`
- Inserção em transação única (`@Transactional`). UNIQUE em `end_to_end_id` garante idempotência (P4).
- Duplicata: capturar `DuplicateKeyException` → tratar como idempotente (não propagar 500).

## Adapter de mensageria (RabbitMQ)
- `EventPublisherPort` → `RabbitEventPublisher` publicando `PixRecebido` (JSON) numa exchange
  `pix.exchange` com routing key `pix.recebido`.
- **Best-effort (P6):** publica após o commit da persistência; falha de publish é logada como
  débito conhecido (sem outbox). Documentar o risco no PR.

## Contrato
- springdoc-openapi → `/v3/api-docs` e Swagger UI. Versionar sob `/v1` (ex: `POST /v1/pix`)? 
  → **Decisão:** prefixo `/v1` para permitir versionamento futuro.

## Pontos de atenção VT
- Este endpoint quase não bloqueia (insert rápido + publish). É esperado que platform threads
  aguente bem aqui — o contraste aparece na 003. **Não** "consertar" isso; é o design.
