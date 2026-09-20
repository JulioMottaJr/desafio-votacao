package br.com.desafiovotacao.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException(Long pautaId) {
        super("Associado já votou nesta pauta. pautaId=" + pautaId);
    }
}
