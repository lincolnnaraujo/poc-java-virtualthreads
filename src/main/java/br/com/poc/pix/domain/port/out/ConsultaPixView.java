package br.com.poc.pix.domain.port.out;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projecao de leitura (read model) do ciclo de vida de um Pix.
 * Desnormalizada: uma linha por endToEndId, com timestamps por etapa.
 */
public record ConsultaPixView(
        String endToEndId,
        String status,
        BigDecimal valor,
        String pagadorNome,
        String recebedorNome,
        Instant recebidoEm,
        Instant processandoEm,
        Instant finalizadoEm,
        String motivoRejeicao,
        Instant atualizadoEm) {
}
