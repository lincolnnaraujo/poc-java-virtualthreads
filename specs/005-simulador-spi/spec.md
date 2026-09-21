# Spec — Fatia 005: Simulador do Autorizador SPI

## Objetivo (WHAT)
Um serviço HTTP **separado** (container próprio) que simula o autorizador do SPI/Pix, com
**latência e taxa de falha configuráveis** em runtime. É o **I/O bloqueante controlado** que
torna o benchmark de virtual threads honesto e reproduzível.

## Justificativa (WHY)
Sem um ponto de bloqueio que **escala** (diferente do pool fixo do Postgres), platform e virtual
threads empatam e a POC não prova nada (`constitution.md` P3). O simulador é essa variável
controlada — e permite as rodadas "limpa" (0% falha) e "caos" (X% falha).

## Escopo
- Endpoint `POST /spi/autorizar` que recebe a transação e responde `AUTORIZADO`/`REJEITADO`.
- **Latência configurável:** fixa ou distribuição (ex: média 100 ms, jitter). Config via propriedade/ambiente e/ou header por requisição.
- **Taxa de falha configurável:** % de `5xx`, % de timeout (resposta que estoura o limite do cliente).
- Endpoint de administração para ajustar latência/falha sem redeploy (ex: `POST /spi/config`).
- Leve e VT-friendly (não deve ser o gargalo por CPU próprio).

## Fora de escopo
- Regra real de autorização Pix; persistência.

## Critérios de Aceitação (Gherkin)

### Cenário 1: resposta com latência configurada
- **Dado** latência configurada em 100 ms e falha 0%
- **Quando** chamo `POST /spi/autorizar`
- **Então** recebo `200 AUTORIZADO` após ~100 ms.

### Cenário 2: taxa de falha aplicada
- **Dado** taxa de falha configurada em 20%
- **Quando** faço N chamadas
- **Então** ~20% retornam `5xx`/timeout (dentro de margem estatística).

### Cenário 3: reconfiguração em runtime
- **Dado** o simulador rodando
- **Quando** chamo `POST /spi/config` alterando latência/falha
- **Então** as chamadas seguintes refletem a nova configuração, sem restart.

## Handoff QA
- QA cobre: aderência estatística da taxa de falha, distribuição de latência, efeito da reconfig.
