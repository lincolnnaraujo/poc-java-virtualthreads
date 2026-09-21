package br.com.poc.pix.domain;

import br.com.poc.pix.domain.model.ChavePix;
import br.com.poc.pix.domain.model.CpfCnpj;
import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.model.Ispb;
import br.com.poc.pix.domain.model.Pagador;
import br.com.poc.pix.domain.model.Recebedor;
import br.com.poc.pix.domain.model.StatusPix;
import br.com.poc.pix.domain.model.TipoChave;
import br.com.poc.pix.domain.model.TipoConta;
import br.com.poc.pix.domain.model.TransacaoPix;
import br.com.poc.pix.domain.model.Valor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes de dominio SEM framework (constitution P1): apenas JUnit + tipos de dominio.
 * Se este teste precisasse de Spring/JDBC/Rabbit para rodar, o hexagono teria vazado.
 */
class DominioPixTest {

    private static final String E2E_VALIDO = "E" + "0123456789".repeat(3) + "0"; // 32 chars

    @Test
    void criaTransacaoNoEstadoRecebido() {
        TransacaoPix transacao = TransacaoPix.receber(
                new EndToEndId(E2E_VALIDO),
                "TX0001",
                new Valor(new BigDecimal("10.00")),
                pagadorValido(),
                recebedorValido(),
                "pagamento pedido 1");

        assertEquals(StatusPix.RECEBIDO, transacao.status());
        assertEquals(E2E_VALIDO, transacao.endToEndId().valor());
    }

    @Test
    void rejeitaValorZeroOuNegativo() {
        assertThrows(PixInvalidoException.class, () -> new Valor(BigDecimal.ZERO));
        assertThrows(PixInvalidoException.class, () -> new Valor(new BigDecimal("-1.00")));
    }

    @Test
    void rejeitaValorComMaisDeDuasCasas() {
        assertThrows(PixInvalidoException.class, () -> new Valor(new BigDecimal("10.001")));
    }

    @Test
    void rejeitaEndToEndIdForaDoFormato() {
        assertThrows(PixInvalidoException.class, () -> new EndToEndId("XPTO"));
        assertThrows(PixInvalidoException.class, () -> new EndToEndId(null));
    }

    @Test
    void rejeitaCpfCnpjInvalido() {
        assertThrows(PixInvalidoException.class, () -> new CpfCnpj("123"));
        assertThrows(PixInvalidoException.class, () -> new CpfCnpj("abcdefghijk"));
    }

    @Test
    void rejeitaIspbForaDeOitoDigitos() {
        assertThrows(PixInvalidoException.class, () -> new Ispb("123"));
    }

    private static Pagador pagadorValido() {
        return new Pagador("Fulano de Tal", new CpfCnpj("12345678901"),
                new Ispb("12345678"), "0001", "1234567", TipoConta.CACC);
    }

    private static Recebedor recebedorValido() {
        return new Recebedor("Ciclano Souza", new CpfCnpj("98765432100"),
                new Ispb("87654321"), "0002", "7654321", TipoConta.CACC,
                new ChavePix("ciclano@email.com"), TipoChave.EMAIL);
    }
}
