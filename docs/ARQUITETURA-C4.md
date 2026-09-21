# Arquitetura — C4 Model

Diagramas no padrão [C4 Model](https://c4model.com/) (Contexto → Container → Componente),
em **Mermaid** (renderiza direto no GitHub/GitLab e na maioria das IDEs). Se o seu visualizador
não renderizar Mermaid, cole os blocos em https://mermaid.live.

> **Nível 4 (Code) é intencionalmente omitido** — como recomenda o C4, o próprio código é a fonte
> de verdade. Ver os pacotes em `src/main/java/br/com/poc/pix/` e as specs em `specs/`.

---

## Nível 1 — Contexto

Quem usa o sistema e com quais sistemas externos ele conversa.

```mermaid
C4Context
    title Nivel 1 - Contexto: POC Pix (Virtual Threads)

    Person(originador, "Originador do Pix", "PSP/cliente que envia a ordem de pagamento e consulta o status")
    Person(eng, "Engenheiro / Apresentador", "Estuda e demonstra o comportamento das virtual threads")

    System(pix, "POC Pix (Virtual Threads)", "Registra, processa (assincrono) e consulta transacoes Pix simuladas")

    System_Ext(spi, "Autorizador SPI (simulado)", "Autoriza a transacao; latencia e taxa de falha configuraveis")

    Rel(originador, pix, "Registra e consulta Pix", "HTTPS/JSON")
    Rel(pix, spi, "Solicita autorizacao", "HTTPS/JSON")
    Rel(eng, pix, "Observa metricas e dashboards", "HTTP")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

---

## Nível 2 — Container

As unidades executáveis/deployáveis e como se comunicam. Cada uma é um serviço no `docker-compose`.

```mermaid
C4Container
    title Nivel 2 - Container: POC Pix

    Person(originador, "Originador do Pix", "PSP/cliente")
    Person(eng, "Engenheiro", "Apresentacao")
    System_Ext(spi, "Autorizador SPI (simulado)", "Java 21 / Spring Boot")

    Container_Boundary(sys, "POC Pix") {
        Container(app, "Aplicacao Pix", "Java 21 / Spring Boot MVC", "API REST + consumer + projecao (arquitetura hexagonal)")
        ContainerDb(db, "PostgreSQL", "PostgreSQL 16", "Write model normalizado + read model (CQRS)")
        ContainerQueue(mq, "RabbitMQ", "RabbitMQ 3", "Eventos de dominio + Dead Letter Queue")
        Container(prom, "Prometheus", "Prometheus", "Coleta de metricas (scrape)")
        Container(graf, "Grafana", "Grafana", "Dashboards do benchmark")
    }

    Container(load, "Gerador de carga", "Gatling / Load.java (JDK 21)", "Teste de carga do benchmark")

    Rel(originador, app, "POST/GET /v1/pix", "HTTPS/JSON")
    Rel(load, app, "Carga (POST/GET)", "HTTPS/JSON")
    Rel(app, db, "Le e grava", "JDBC")
    Rel(app, mq, "Publica e consome eventos", "AMQP")
    Rel(app, spi, "Autoriza transacao", "HTTPS/JSON")
    Rel(prom, app, "Raspa /actuator/prometheus", "HTTP")
    Rel(prom, spi, "Raspa metricas", "HTTP")
    Rel(prom, mq, "Raspa metricas (plugin)", "HTTP")
    Rel(graf, prom, "Consulta metricas", "HTTP")
    Rel(eng, graf, "Analisa dashboards", "HTTP")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

---

## Nível 3 — Componente (dentro da "Aplicação Pix")

O hexágono (Ports & Adapters). **Regra de dependência:** adapters → aplicação → domínio. O núcleo
de domínio não conhece framework (constitution P1).

```mermaid
C4Component
    title Nivel 3 - Componente: Aplicacao Pix (hexagono)

    Person(originador, "Originador", "PSP/cliente")
    ContainerDb(db, "PostgreSQL", "", "write + read model")
    ContainerQueue(mq, "RabbitMQ", "", "eventos + DLQ")
    System_Ext(spi, "Autorizador SPI", "", "simulado")

    Container_Boundary(app, "Aplicacao Pix") {
        Component(cmdCtrl, "PixCommandController", "adapter.in.web", "POST /v1/pix (202)")
        Component(qryCtrl, "PixQueryController", "adapter.in.web", "GET /v1/pix/{id}")
        Component(recListener, "PixRecebidoListener", "adapter.in.messaging", "consome PixRecebido; DLQ")
        Component(projListener, "ProjecaoPixListener", "adapter.in.messaging", "@RabbitHandler por tipo")

        Component(regSvc, "RegistrarPixService", "application", "valida, persiste, publica")
        Component(procSvc, "ProcessarPixService", "application", "idempotencia, SPI, estado")
        Component(consSvc, "ConsultarPixService", "application", "le o read model")
        Component(projSvc, "ProjecaoPixService", "application", "aplica eventos ao read model")

        Component(dominio, "Dominio Pix", "domain (sem framework)", "TransacaoPix + Value Objects + Portas")

        Component(repo, "PixRepositoryJdbc", "adapter.out.persistence", "write model (JdbcTemplate)")
        Component(consRepo, "ConsultaPixJdbc", "adapter.out.persistence", "read model + projecao (upsert)")
        Component(pub, "RabbitEventPublisher", "adapter.out.messaging", "publica eventos (best-effort)")
        Component(spiClient, "AutorizadorSpiClient", "adapter.out.spi", "HttpClient + Resilience4j")
    }

    Rel(originador, cmdCtrl, "POST", "JSON")
    Rel(originador, qryCtrl, "GET", "JSON")

    Rel(cmdCtrl, regSvc, "registra")
    Rel(regSvc, dominio, "constroi agregado")
    Rel(regSvc, repo, "salva (RECEBIDO)")
    Rel(regSvc, pub, "publica PixRecebido")

    Rel(pub, mq, "AMQP")
    Rel(mq, recListener, "entrega PixRecebido")
    Rel(recListener, procSvc, "processa")
    Rel(procSvc, spiClient, "autoriza")
    Rel(spiClient, spi, "HTTPS")
    Rel(procSvc, repo, "atualiza status")
    Rel(procSvc, pub, "publica resultado")

    Rel(mq, projListener, "entrega eventos")
    Rel(projListener, projSvc, "projeta")
    Rel(projSvc, consRepo, "upsert read model")
    Rel(consSvc, consRepo, "le")

    Rel(repo, db, "JDBC")
    Rel(consRepo, db, "JDBC")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

---

## Legenda dos níveis C4

| Nível | Pergunta que responde | Público |
|---|---|---|
| **1 — Contexto** | Como o sistema se encaixa no mundo? Quem usa, com o que conversa? | Todos (inclusive não-técnicos) |
| **2 — Container** | Quais as peças executáveis e como se comunicam? | Time técnico / arquitetura |
| **3 — Componente** | Como cada container é organizado por dentro? | Desenvolvedores |
| 4 — Code | (omitido) O código é a fonte de verdade | — |

## Notas de arquitetura
- **Bounded Context:** `Pagamento Pix`. Agregado raiz: `TransacaoPix` (identidade = `endToEndId`).
- **CQRS-lite:** write model normalizado (`transacao_pix` + `pagador` + `recebedor`) e read model
  desnormalizado (`consulta_pix`), na mesma instância Postgres, ligados por eventos.
- **Mensageria atrás de porta:** trocar RabbitMQ por SQS = trocar adapter, sem tocar no domínio.
- **Toggles do benchmark:** HTTP (`spring.threads.virtual.enabled`) e consumer
  (`benchmark.consumer.mode`) — ver `benchmark/RELATORIO.md`.
