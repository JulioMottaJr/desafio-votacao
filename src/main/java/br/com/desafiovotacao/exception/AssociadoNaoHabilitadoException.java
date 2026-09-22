package br.com.desafiovotacao.exception;

public class AssociadoNaoHabilitadoException extends RuntimeException {

    public AssociadoNaoHabilitadoException() {
        super("Associado não habilitado para votar.");
    }
}
