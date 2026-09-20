package br.com.desafiovotacao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PautaNaoEncontradaException extends RuntimeException {

    public PautaNaoEncontradaException(Long pautaId) {
        super("Pauta não encontrada. pautaId=" + pautaId);
    }
}
