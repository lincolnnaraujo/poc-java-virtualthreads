# Spec — Fatia 002: Comando `POST /pix`

## Objetivo (WHAT)
Expor o endpoint de comando que recebe um **Pix completo e realista**, valida, persiste o estado
inicial `RECEBIDO` no modelo de escrita **normalizado** e publica um evento no RabbitMQ para
processamento assíncrono. Responde `202 Accepted`.

## Justificativa (WHY)
É a porta de entrada do fluxo. Precisa ser rápida (quase sem bloqueio) para que a fatia de
benchmark evidencie que o gargalo de I/O real está no **consumer** (fatia 003), não aqui.

## Escopo
- DTO de request realista de Pix (ver payload abaixo) + validação (`jakarta.validation`).
- Agregado `TransacaoPix` (identidade = `endToEndId`).
- Persistência via `JdbcTemplate` em tabelas normalizadas (`transacao_pix`, `pagador`, `recebedor`).
- **Idempotência:** UNIQUE em `end_to_end_id` (P4); duplicata → `200/409` idempotente, sem duplicar.
- Publicação de evento `PixRecebido` via `EventPublisherPort` (adapter RabbitMQ) — **best-effort** (P6).
- Resposta `202 Accepted` com `endToEndId` e `Location: /pix/{endToEndId}`.
- Contrato OpenAPI (springdoc) do endpoint.

## Payload de Pix (realista)
```json
{
  "endToEndId": "E1234567820260921153000abcdef123",
  "txid": "PoC0001TXID",
  "valor": 150.75,
  "pagador":   { "nome": "...", "cpfCnpj": "...", "ispb": "12345678", "agencia": "0001", "conta": "1234567", "tipoConta": "CACC" },
  "recebedor": { "nome": "...", "cpfCnpj": "...", "ispb": "87654321", "agencia": "0002", "conta": "7654321", "tipoConta": "CACC",
                 "chave": "...", "tipoChave": "EMAIL" },
  "infoEntreClientes": "Pagamento pedido 123"
}
```

## Fora de escopo
- Processamento/autorização (fatia 003), consulta (004).

## Critérios de Aceitação (Gherkin)

### Cenário 1: Pix válido é aceito
- **Dado** um payload de Pix válido e completo
- **Quando** chamo `POST /pix`
- **Então** recebo `202 Accepted` com `endToEndId` e header `Location`
- **E** existe um registro `RECEBIDO` nas tabelas de escrita
- **E** um evento `PixRecebido` foi publicado no RabbitMQ.

### Cenário 2 (erro): payload inválido é rejeitado
- **Dado** um payload sem `endToEndId` ou com `valor <= 0`
- **Quando** chamo `POST /pix`
- **Então** recebo `400 Bad Request` com detalhamento dos campos inválidos
- **E** nada é persistido nem publicado.

### Cenário 3: idempotência no comando
- **Dado** um `endToEndId` já registrado
- **Quando** chamo `POST /pix` novamente com o mesmo `endToEndId`
- **Então** a resposta é idempotente (não cria duplicado; retorna `200`/`409` conforme decisão)
- **E** não há segundo evento publicado.

## Handoff QA
- QA cobre: matriz de validação de campos, mascaramento de PII no log do request, idempotência.
