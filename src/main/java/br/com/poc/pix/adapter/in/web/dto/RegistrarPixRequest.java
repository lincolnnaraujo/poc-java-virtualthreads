package br.com.poc.pix.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload de entrada do POST /v1/pix. Validacao de formato via Bean Validation (400);
 * invariantes de dominio sao reforcadas nos Value Objects (defesa em profundidade).
 */
public record RegistrarPixRequest(

        @NotBlank(message = "endToEndId e obrigatorio")
        String endToEndId,

        String txid,

        @NotNull(message = "valor e obrigatorio")
        @Positive(message = "valor deve ser maior que zero")
        BigDecimal valor,

        @Size(max = 140, message = "infoEntreClientes deve ter no maximo 140 caracteres")
        String infoEntreClientes,

        @NotNull(message = "pagador e obrigatorio")
        @Valid
        Pagador pagador,

        @NotNull(message = "recebedor e obrigatorio")
        @Valid
        Recebedor recebedor) {

    public record Pagador(
            @NotBlank(message = "pagador.nome e obrigatorio") String nome,
            @NotBlank(message = "pagador.cpfCnpj e obrigatorio") String cpfCnpj,
            @NotBlank(message = "pagador.ispb e obrigatorio") String ispb,
            String agencia,
            @NotBlank(message = "pagador.conta e obrigatoria") String conta,
            @NotBlank(message = "pagador.tipoConta e obrigatorio") String tipoConta) {
    }

    public record Recebedor(
            @NotBlank(message = "recebedor.nome e obrigatorio") String nome,
            @NotBlank(message = "recebedor.cpfCnpj e obrigatorio") String cpfCnpj,
            @NotBlank(message = "recebedor.ispb e obrigatorio") String ispb,
            String agencia,
            @NotBlank(message = "recebedor.conta e obrigatoria") String conta,
            @NotBlank(message = "recebedor.tipoConta e obrigatorio") String tipoConta,
            @NotBlank(message = "recebedor.chave e obrigatoria") String chave,
            @NotBlank(message = "recebedor.tipoChave e obrigatorio") String tipoChave) {
    }
}
