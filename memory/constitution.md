# Constituição do Projeto — POC Virtual Threads (Pix)

> Documento **normativo** da POC. Toda spec, plano e tarefa DEVE respeitar estes princípios.
> Formato inspirado no fluxo SDD (Spec-Driven Development / GitHub Spec Kit).
> Alterações aqui exigem consenso — não é decisão de implementação, é decisão de projeto.

---

## Propósito

POC de **estudo e demonstração** do uso de **Virtual Threads (Java 21)** sob alto TPS,
simulando uma **API de Pix**, para **apresentação a um time técnico de desenvolvedores sêniores**.
O entregável não é "um Pix que funciona" — é um **benchmark honesto e reproduzível** que
evidencie o ganho de virtual threads vs platform threads, e o material para explicá-lo.

---

## Princípios Não-Negociáveis

### P1 — Fronteiras Hexagonais Rígidas
O núcleo de domínio (use cases, entidades, regras do Pix) **NÃO pode importar** Spring,
JDBC, RabbitMQ ou qualquer detalhe de infraestrutura. Dependências apontam **para dentro**.
Se um teste de regra de negócio precisar subir `ApplicationContext`, o hexágono vazou.

### P2 — Código "Virtual-Thread-Friendly"
- **Proibido** `synchronized` em trechos que executam I/O (bloqueio → *pinning* da carrier thread).
  Usar `java.util.concurrent.locks.ReentrantLock` quando exclusão mútua for necessária.
- Preferir bibliotecas que não segurem monitores durante I/O.
- O driver Postgres (pgJDBC) usado deve ser versão VT-friendly.

### P3 — Honestidade do Benchmark
- **Uma variável por vez.** Platform vs Virtual muda **apenas** o toggle; todo o resto é idêntico.
- Mesma carga, mesmo hardware, mesma contenção (todos os containers na mesma máquina).
- **Número real vai pro slide** — se o baseline provar valor diferente do esperado, documenta-se o real.
- Toda alegação de performance tem baseline + método reproduzível descrito.

### P4 — Idempotência
`endToEndId` é a chave de idempotência do Pix. Persistência de escrita tem **UNIQUE constraint**
em `endToEndId`. Duplicata (entrega at-least-once do RabbitMQ) → detectada por violação de
constraint → ACK e descarte, sem efeito colateral.

### P5 — Segurança Mínima de PII
- CPF/CNPJ e chave Pix **NUNCA** em log em texto claro. Máscara obrigatória (ex: `***.***.**9-00`).
- Autenticação/autorização está **fora de escopo** (declarado), não esquecido.

### P6 — Débito Técnico Declarado, Não Acidental
- **Dual-write best-effort:** o `POST` persiste `RECEBIDO` e publica no RabbitMQ **sem** garantia
  atômica. Risco de perda/duplicação de evento é **conhecido e aceito** para esta POC.
  Mitigação real (Transactional Outbox) é melhoria futura registrada.

### P7 — Acesso a Dados
JDBC puro via `JdbcTemplate`. **Sem JPA/Hibernate** — controle explícito de conexão para
medir pool e pinning honestamente.

### P8 — Stack Congelada
Java 21 · Spring Boot · Maven · PostgreSQL · RabbitMQ · Docker Compose · Gatling ·
Micrometer + Prometheus + Grafana · Resilience4j. Trocas exigem alterar esta constituição.

### P9 — Testabilidade é do QA
A estratégia formal de testes (contrato, integração, cenários) é **responsabilidade explícita
do QA do time**. Devs entregam: (a) domínio testável em isolamento e (b) `docker-compose`
que sobe tudo para o QA exercitar. Ver seção "Handoff QA" no README.

---

## Decisão Registrada: RabbitMQ (e por que não SQS/Kafka)

| Candidato | Veredito | Motivo |
|-----------|----------|--------|
| **RabbitMQ** ✅ | **Escolhido** | 100% local, rápido, determinístico, DLQ nativa, **não contamina a medição**. Semântica comando→processamento limpa. |
| SQS (AWS) ❌ | Rejeitado p/ benchmark | LocalStack não é representativo de performance; SQS FIFO tem teto de ~300 TPS; AWS real injeta latência de rede como variável confundidora. |
| Kafka ❌ | Rejeitado | Overkill; semântica de partição vira ruído na apresentação. |

**Salvaguarda arquitetural:** a mensageria fica **atrás de uma porta** (`EventPublisherPort` /
`EventConsumerPort`). Trocar RabbitMQ por SQS = trocar o adapter, **sem tocar no domínio**.
SQS fica registrado como *adapter futuro*.

---

## Envelope de Recursos (reprodutibilidade)

Máquina de referência da demo: **Intel i7-11800H — 8 núcleos físicos / 16 lógicos, 29 GB RAM**.
Todos os componentes co-locados. Limites por container:

| Componente            | vCPU | Memória            |
|-----------------------|------|--------------------|
| App (sujeito do teste)| 4    | 1 GB heap / 1.5 GB |
| PostgreSQL            | 2    | 2 GB               |
| RabbitMQ              | 2    | 1 GB               |
| Simulador SPI         | 2    | 512 MB             |
| Prometheus + Grafana  | 2    | 1.5 GB             |
| Gatling + SO          | ~4 lógicos | restante     |

---

## Definition of Ready (DoR) — base do épico
- [ ] História com valor e critério de aceitação testável.
- [ ] Abordagem técnica alinhada a esta constituição.
- [ ] Dependências de infra (containers) identificadas.
- [ ] RNF aplicáveis definidos.
- [ ] Sem impedimento bloqueante.

## Definition of Done (DoD) — base do épico
- [ ] Código conforme critérios de aceitação e princípios P1–P9.
- [ ] Domínio testável sem framework (P1).
- [ ] Sem `synchronized` em I/O; verificação de pinning limpa quando aplicável (P2).
- [ ] PII mascarada nos logs (P5).
- [ ] `docker-compose up` sobe a fatia de ponta a ponta.
- [ ] Documentação da fatia (README/spec) atualizada e **direta** (repo compartilhado).
- [ ] Métricas expostas no Prometheus/Grafana quando aplicável.
- [ ] Handoff para QA registrado (cenários que o QA deve cobrir).
