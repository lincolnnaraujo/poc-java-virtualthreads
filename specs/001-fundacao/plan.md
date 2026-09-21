# Plano Técnico — Fatia 001: Fundação

## Estrutura de pacotes (hexagonal)
```
br.com.poc.pix
├── domain            # entidades, VOs, regras — SEM imports de framework (P1)
│   ├── model
│   └── port
│       ├── in        # use cases (interfaces)
│       └── out       # portas de saída (interfaces)
├── application       # implementação dos use cases (orquestração)
└── adapter
    ├── in.web        # controllers REST
    └── out
        ├── persistence   # JdbcTemplate + repos
        └── messaging     # RabbitMQ publisher/consumer
```
Regra de dependência: `adapter` → `application` → `domain`. Nunca o inverso.

## Maven (`pom.xml`) — dependências-chave
- `spring-boot-starter-web` (MVC servlet)
- `spring-boot-starter-jdbc` (JdbcTemplate — **sem** `data-jpa`)
- `spring-boot-starter-actuator`
- `spring-boot-starter-amqp` (RabbitMQ)
- `org.postgresql:postgresql`
- `org.flywaydb:flyway-core` + `flyway-database-postgresql`
- `spring-boot-starter-validation`
- (fatias futuras adicionam: resilience4j, micrometer-registry-prometheus)
- Java version: `<java.version>21</java.version>`

## docker-compose.yml (serviços)
| Serviço  | Imagem                         | Portas        | Limites (P/ envelope) |
|----------|--------------------------------|---------------|-----------------------|
| postgres | `postgres:16-alpine`           | 5432          | cpus 2 / 2g           |
| rabbitmq | `rabbitmq:3-management-alpine` | 5672, 15672   | cpus 2 / 1g           |
| app      | build local (Dockerfile)       | 8080          | cpus 4 / 1.5g         |

- `healthcheck` em cada serviço; `app` com `depends_on: condition: service_healthy`.
- Rede bridge dedicada; volumes nomeados para o Postgres.
- Config via variáveis de ambiente (`SPRING_DATASOURCE_URL`, `SPRING_RABBITMQ_*`).

## Dockerfile (app)
- Multi-stage: build Maven (JDK 21) → runtime `eclipse-temurin:21-jre`.
- `JAVA_TOOL_OPTIONS` para heap (1g) parametrizável.

## Flyway
- `V1__baseline.sql` (vazio ou extensão `pgcrypto` se necessário). Tabelas reais entram na 002/004.

## Observações VT
- Toggle de virtual threads **não** é ligado aqui; entra na 007. Mas o baseline já roda em MVC
  servlet para que ligar `spring.threads.virtual.enabled` depois seja trivial.

## Riscos
- `depends_on healthy` mal configurado → app sobe antes do DB. Mitigar com healthchecks reais.
