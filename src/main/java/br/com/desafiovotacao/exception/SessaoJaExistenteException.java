package br.com.desafiovotacao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SessaoJaExistenteException extends RuntimeException {

    public SessaoJaExistenteException(Long pautaId) {
        super("Pauta já possui sessão de votação. pautaId=" + pautaId);
    }
}
