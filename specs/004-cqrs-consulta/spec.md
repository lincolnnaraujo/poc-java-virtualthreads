# Spec — Fatia 004: CQRS + Consulta do Ciclo de Vida

## Objetivo (WHAT)
Manter uma **base de leitura desnormalizada** atualizada por eventos e expor
`GET /v1/pix/{endToEndId}` para acompanhar o **ciclo de vida** do Pix
(`RECEBIDO` → `EM_PROCESSAMENTO` → `AUTORIZADO`/`REJEITADO`/`DEVOLVIDO`), com timestamps por etapa.

## Justificativa (WHY)
O usuário pediu explicitamente acompanhar a etapa de processamento. Separar leitura de escrita
(CQRS-lite) permite consulta rápida e desacoplada, e adiciona um segundo ponto de leitura de alta
concorrência para o benchmark (fatia 007).

## Escopo
- Tabela de leitura `consulta_pix` (desnormalizada, uma linha por `endToEndId`).
- **Projeção:** handlers que consomem `PixRecebido`, `PixAutorizado`, `PixRejeitado` e fazem
  upsert no read model, registrando a etapa e o timestamp.
- `GET /v1/pix/{endToEndId}` → status atual + histórico de etapas.
- **Consistência eventual** aceita (P/ constitution): consulta pode retornar estado intermediário.

## Fora de escopo
- Geração de carga (007). Simulador SPI (005).

## Critérios de Aceitação (Gherkin)

### Cenário 1: consulta reflete estado final
- **Dado** um Pix processado com sucesso
- **Quando** chamo `GET /v1/pix/{endToEndId}`
- **Então** recebo `200` com status `AUTORIZADO` e os timestamps de `RECEBIDO`/`EM_PROCESSAMENTO`/`AUTORIZADO`.

### Cenário 2: consistência eventual (estado intermediário)
- **Dado** um Pix recebido mas ainda não autorizado
- **Quando** consulto imediatamente
- **Então** recebo `200` com status `RECEBIDO` ou `EM_PROCESSAMENTO` (não erro).

### Cenário 3 (erro): endToEndId inexistente
- **Dado** um `endToEndId` desconhecido
- **Quando** consulto
- **Então** recebo `404 Not Found` (problem+json).

## Handoff QA
- QA cobre: corrida consulta-vs-processamento, projeção idempotente, 404, formato de resposta.
