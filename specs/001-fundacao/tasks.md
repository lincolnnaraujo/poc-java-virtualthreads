# Tarefas — Fatia 001: Fundação

- [x] T1.1 — Criar projeto Maven: Java 21, Spring Boot 3.4.1, deps web/jdbc/actuator/amqp/validation/flyway/postgresql.
- [x] T1.2 — Configurar `<java.version>21</java.version>` e `spring-boot-maven-plugin`.
- [x] T1.3 — Criar estrutura de pacotes hexagonal (`domain`, `application`, `adapter.in.web`, `adapter.out.persistence`, `adapter.out.messaging`) com `package-info` documentando cada camada.
- [x] T1.4 — Adicionar teste de arquitetura (ArchUnit) garantindo que `domain` não importa framework/adapters/application (P1).
- [x] T1.5 — Escrever `Dockerfile` multi-stage (build Maven+JDK 21 → runtime JRE 21 + curl p/ healthcheck).
- [x] T1.6 — Escrever `docker-compose.yml` com `postgres`, `rabbitmq`, `app` + healthchecks + limites de recurso (envelope da constitution).
- [x] T1.7 — Externalizar config (`application.yml` + variáveis de ambiente); sem segredo hardcoded.
- [x] T1.8 — Configurar Flyway + `V1__baseline.sql`.
- [x] T1.9 — Habilitar Actuator health com detalhes de `db` e `rabbit`.
- [x] T1.10 — Validar: `docker compose up -d` → health `UP` (200, db+rabbit UP); Postgres parado → health `DOWN` (503); recupera para `UP` ao religar. **✅ verificado**
- [x] T1.11 — Documentar "como rodar" no README.

## Pronto quando
- [x] Cenários 1 e 3 do `spec.md` verdes (validados via `docker compose`).
- [x] Build da imagem compila e empacota o jar (verificado).
- [~] Cenário 2 (ArchUnit): teste wired e compilando; enforcement torna-se significativo na fatia 002 (domínio hoje só tem `package-info`).

## Notas de verificação (2026-09-21)
- `docker compose build app`: **OK** — `mvn -DskipTests package` gerou `pix-poc-0.0.1-SNAPSHOT.jar`.
- `docker compose up -d`: postgres/rabbitmq **healthy**; app iniciou só após ambos (`depends_on: service_healthy`).
- `GET /actuator/health` → **200 UP** (`db`=PostgreSQL UP, `rabbit`=3.13.7 UP).
- Postgres parado → **503 DOWN** (db DOWN) → religado → **200 UP**.
- Ambiente: JDK 25 + Docker 29 / Compose v5 no host; Maven **não** instalado localmente (build via Docker).
