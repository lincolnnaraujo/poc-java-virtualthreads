# Spec — Fatia 001: Fundação

## Objetivo (WHAT)
Estabelecer o esqueleto do projeto: aplicação Spring Boot 21 hexagonal (Maven), com
`docker-compose` subindo a app + PostgreSQL + RabbitMQ, migrações versionadas e health check.
**Nenhuma regra de Pix ainda** — só o chão firme para as fatias seguintes.

## Justificativa (WHY)
Todas as fatias dependem de: build reproduzível, estrutura hexagonal correta desde o dia 1,
e infra local de pé. Sem isso, as fatias de negócio não têm onde rodar.

## Escopo
- Projeto Maven, Java 21, Spring Boot Web MVC.
- Estrutura de pacotes hexagonal (domínio isolado — `constitution.md` P1).
- `docker-compose.yml` com: `app`, `postgres`, `rabbitmq` (com management UI).
- Migração de schema versionada (Flyway) — inicialmente vazia/baseline.
- Endpoint `GET /actuator/health` respondendo `UP` com app + DB + RabbitMQ.
- `.env`/config externalizada (sem segredo hardcoded).

## Fora de escopo
- Endpoints de Pix, consumer, SPI, observabilidade, Gatling (fatias 002–007).

## Critérios de Aceitação (Gherkin)

### Cenário 1: Ambiente sobe de ponta a ponta
- **Dado** um checkout limpo do repositório
- **Quando** executo `docker compose up -d --build`
- **Então** os containers `app`, `postgres` e `rabbitmq` ficam saudáveis
- **E** `GET http://localhost:8080/actuator/health` retorna `200` e status `UP`.

### Cenário 2: Estrutura hexagonal íntegra
- **Dado** o código-fonte
- **Quando** inspeciono o pacote de domínio
- **Então** ele **não** importa Spring, JDBC ou RabbitMQ (P1).

### Cenário 3 (erro): dependência indisponível é reportada
- **Dado** o Postgres parado
- **Quando** consulto o health
- **Então** o status geral é `DOWN` com o detalhe do componente `db` como `DOWN`.

## Dependências
- Docker + Docker Compose; JDK 21 para build local.

## Handoff QA
- Fatia de infra — QA valida que `docker compose up` sobe tudo em máquina limpa.
