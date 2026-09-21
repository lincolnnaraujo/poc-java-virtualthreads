package br.com.poc.pix.domain.port.in;

/**
 * Porta de entrada: registrar um Pix recebido (comando do fluxo assincrono).
 */
public interface RegistrarPixUseCase {
    RegistroPixResultado registrar(RegistrarPixCommand comando);
}
