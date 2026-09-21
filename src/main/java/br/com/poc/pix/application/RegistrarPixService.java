package br.com.poc.pix.application;

import br.com.poc.pix.domain.PixJaRegistradoException;
import br.com.poc.pix.domain.event.PixRecebido;
import br.com.poc.pix.domain.model.ChavePix;
import br.com.poc.pix.domain.model.CpfCnpj;
import br.com.poc.pix.domain.model.EndToEndId;
import br.com.poc.pix.domain.model.Ispb;
import br.com.poc.pix.domain.model.Pagador;
import br.com.poc.pix.domain.model.Recebedor;
import br.com.poc.pix.domain.model.TipoChave;
import br.com.poc.pix.domain.model.TipoConta;
import br.com.poc.pix.domain.model.TransacaoPix;
import br.com.poc.pix.domain.model.Valor;
import br.com.poc.pix.domain.port.in.RegistrarPixCommand;
import br.com.poc.pix.domain.port.in.RegistrarPixUseCase;
import br.com.poc.pix.domain.port.in.RegistroPixResultado;
import br.com.poc.pix.domain.port.out.EventPublisherPort;
import br.com.poc.pix.domain.port.out.PixRepositoryPort;
import br.com.poc.pix.domain.PixInvalidoException;
import org.springframework.stereotype.Service;

/**
 * Orquestra o registro de um Pix: converte o comando em agregado (validando invariantes),
 * persiste (idempotente por endToEndId) e publica o evento {@link PixRecebido} best-effort.
 *
 * <p>Nota de projeto: a publicacao ocorre <b>apos</b> a persistencia commitada (o
 * {@code salvar} do adapter e transacional), garantindo que o consumer (003) nunca receba
 * evento de uma transacao ainda nao visivel no banco.</p>
 */
@Service
public class RegistrarPixService implements RegistrarPixUseCase {

    private final PixRepositoryPort repositorio;
    private final EventPublisherPort publicador;

    public RegistrarPixService(PixRepositoryPort repositorio, EventPublisherPort publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    @Override
    public RegistroPixResultado registrar(RegistrarPixCommand comando) {
        EndToEndId endToEndId = new EndToEndId(comando.endToEndId());

        // Atalho idempotente: evita construir/inserir se ja existe.
        if (repositorio.existe(endToEndId)) {
            return RegistroPixResultado.jaRegistrado(endToEndId.valor());
        }

        TransacaoPix transacao = montar(comando, endToEndId);

        try {
            repositorio.salvar(transacao);
        } catch (PixJaRegistradoException corrida) {
            // Duas requisicoes concorrentes com o mesmo endToEndId: a UNIQUE constraint
            // garante idempotencia; a perdedora cai aqui e responde como ja registrado.
            return RegistroPixResultado.jaRegistrado(endToEndId.valor());
        }

        publicador.publicar(new PixRecebido(
                transacao.endToEndId().valor(),
                transacao.valor().quantia(),
                transacao.txid(),
                transacao.pagador().nome(),
                transacao.recebedor().nome(),
                transacao.criadoEm()));

        return RegistroPixResultado.registrado(endToEndId.valor());
    }

    private TransacaoPix montar(RegistrarPixCommand cmd, EndToEndId endToEndId) {
        Pagador pagador = new Pagador(
                cmd.pagador().nome(),
                new CpfCnpj(cmd.pagador().cpfCnpj()),
                new Ispb(cmd.pagador().ispb()),
                cmd.pagador().agencia(),
                cmd.pagador().conta(),
                tipoConta(cmd.pagador().tipoConta()));

        Recebedor recebedor = new Recebedor(
                cmd.recebedor().nome(),
                new CpfCnpj(cmd.recebedor().cpfCnpj()),
                new Ispb(cmd.recebedor().ispb()),
                cmd.recebedor().agencia(),
                cmd.recebedor().conta(),
                tipoConta(cmd.recebedor().tipoConta()),
                new ChavePix(cmd.recebedor().chave()),
                tipoChave(cmd.recebedor().tipoChave()));

        return TransacaoPix.receber(
                endToEndId,
                cmd.txid(),
                new Valor(cmd.valor()),
                pagador,
                recebedor,
                cmd.infoEntreClientes());
    }

    private static TipoConta tipoConta(String valor) {
        try {
            return TipoConta.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PixInvalidoException("tipoConta invalido: " + valor + " (esperado CACC, SVGS, SLRY ou TRAN)");
        }
    }

    private static TipoChave tipoChave(String valor) {
        try {
            return TipoChave.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PixInvalidoException("tipoChave invalido: " + valor + " (esperado CPF, CNPJ, EMAIL, TELEFONE ou EVP)");
        }
    }
}
