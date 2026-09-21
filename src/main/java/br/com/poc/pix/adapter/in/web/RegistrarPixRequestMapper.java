package br.com.poc.pix.adapter.in.web;

import br.com.poc.pix.adapter.in.web.dto.RegistrarPixRequest;
import br.com.poc.pix.domain.port.in.RegistrarPixCommand;

/**
 * Traduz o DTO de request (fronteira web) para o comando de dominio.
 */
final class RegistrarPixRequestMapper {

    private RegistrarPixRequestMapper() {
    }

    static RegistrarPixCommand toCommand(RegistrarPixRequest req) {
        return new RegistrarPixCommand(
                req.endToEndId(),
                req.txid(),
                req.valor(),
                req.infoEntreClientes(),
                new RegistrarPixCommand.Pagador(
                        req.pagador().nome(),
                        req.pagador().cpfCnpj(),
                        req.pagador().ispb(),
                        req.pagador().agencia(),
                        req.pagador().conta(),
                        req.pagador().tipoConta()),
                new RegistrarPixCommand.Recebedor(
                        req.recebedor().nome(),
                        req.recebedor().cpfCnpj(),
                        req.recebedor().ispb(),
                        req.recebedor().agencia(),
                        req.recebedor().conta(),
                        req.recebedor().tipoConta(),
                        req.recebedor().chave(),
                        req.recebedor().tipoChave()));
    }
}
