package br.com.poc.pix.common;

/**
 * Utilitario de mascaramento de PII para logs (constitution P5).
 * CPF/CNPJ e chave Pix NUNCA em texto claro nos logs.
 */
public final class Mascaras {

    private Mascaras() {
    }

    /** Mantem apenas os 2 ultimos digitos do documento visiveis. */
    public static String documento(String doc) {
        if (doc == null || doc.length() < 4) {
            return "***";
        }
        return "***" + doc.substring(doc.length() - 2);
    }

    /** Mascara a chave Pix; para e-mails preserva a inicial e o dominio. */
    public static String chave(String chave) {
        if (chave == null || chave.isBlank()) {
            return "***";
        }
        int arroba = chave.indexOf('@');
        if (arroba > 0) {
            return chave.charAt(0) + "***" + chave.substring(arroba);
        }
        if (chave.length() <= 4) {
            return "***";
        }
        return "***" + chave.substring(chave.length() - 4);
    }
}
