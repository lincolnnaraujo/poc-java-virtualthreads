# Plano Técnico — Fatia 005: Simulador SPI

## Decisão: serviço dedicado vs WireMock
- **Escolha:** **serviço Spring Boot minimalista dedicado** (módulo/container próprio).
  - Prós: controle total de latência (distribuição/jitter), taxa de falha, reconfiguração em runtime, métricas próprias.
  - WireMock com response templating foi considerado, mas o controle fino de distribuição de
    latência + reconfig dinâmica fica mais limpo num serviço próprio.
- Deve rodar com **virtual threads habilitado** para sustentar milhares de conexões concorrentes
  sem virar o gargalo do experimento (senão o simulador limita o alvo, não a app).

## API
- `POST /spi/autorizar` → body com `endToEndId`/valor; responde `{ "resultado": "AUTORIZADO|REJEITADO" }`.
- `POST /spi/config` → `{ "latenciaMediaMs": 100, "jitterMs": 30, "taxaFalha": 0.0, "taxaTimeout": 0.0 }`.
- `GET /spi/config` → config atual (para o relatório do benchmark registrar as condições).

## Implementação
- Latência: `Thread.sleep(...)` com jitter (numa virtual thread, sleep não custa carrier).
  Alternativa: atraso via scheduler não-bloqueante — mas `sleep` em VT é **didaticamente ótimo**
  (mostra bloqueio barato). Documentar essa escolha.
- Falha: sorteio uniforme; `taxaTimeout` responde após atraso > timeout do cliente (1s) para
  disparar o `TimeLimiter` do lado do consumer.
- Config em memória (`AtomicReference<Config>`), thread-safe, sem `synchronized` em I/O (P2).

## Infra
- Novo serviço no `docker-compose` (`spi-sim`), limites 2 vCPU / 512 MB.
- App aponta `AUTORIZADOR_SPI_URL` para `http://spi-sim:8081/spi/autorizar`.

## Métricas (para 006)
- Contadores de autorizado/rejeitado/falha e histograma de latência aplicada.
