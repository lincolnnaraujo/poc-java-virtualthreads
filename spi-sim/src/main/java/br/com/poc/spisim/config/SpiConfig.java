package br.com.poc.spisim.config;

/**
 * Configuracao do comportamento do simulador SPI.
 *
 * @param latenciaMediaMs latencia base aplicada nas respostas de sucesso/rejeicao
 * @param jitterMs        variacao aleatoria +/- aplicada sobre a latencia base
 * @param taxaFalha       probabilidade [0..1] de responder 5xx (dispara retry/CB no cliente)
 * @param taxaTimeout     probabilidade [0..1] de estourar o timeout do cliente (dorme timeoutMs)
 * @param taxaRejeicao    probabilidade [0..1] de responder 200 REJEITADO
 * @param timeoutMs       tempo dormido quando o caso de timeout e sorteado
 */
public record SpiConfig(
        long latenciaMediaMs,
        long jitterMs,
        double taxaFalha,
        double taxaTimeout,
        double taxaRejeicao,
        long timeoutMs) {

    public SpiConfig {
        if (latenciaMediaMs < 0 || jitterMs < 0 || timeoutMs < 0) {
            throw new IllegalArgumentException("latencias devem ser >= 0");
        }
        validarTaxa(taxaFalha, "taxaFalha");
        validarTaxa(taxaTimeout, "taxaTimeout");
        validarTaxa(taxaRejeicao, "taxaRejeicao");
        if (taxaFalha + taxaTimeout + taxaRejeicao > 1.0) {
            throw new IllegalArgumentException("a soma de taxaFalha + taxaTimeout + taxaRejeicao nao pode exceder 1.0");
        }
    }

    private static void validarTaxa(double taxa, String nome) {
        if (taxa < 0.0 || taxa > 1.0) {
            throw new IllegalArgumentException(nome + " deve estar entre 0 e 1");
        }
    }
}
