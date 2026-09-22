package br.com.desafiovotacao.client;

/**
 * Representa semanticamente o HTTP 404 (Not Found) do serviço externo de CPF:
 * o CPF foi considerado inválido ou não encontrado, não apenas inapto para votar.
 * O fake local lança esta exceção sem realizar uma chamada HTTP.
 *
 * <p>Não define o status HTTP da API consumidora. Esse mapeamento cabe à camada
 * web quando houver um fluxo HTTP integrado ao Client. Não armazena o CPF recebido.
 */
public class CpfInvalidoException extends RuntimeException {

    public CpfInvalidoException() {
        super("CPF considerado inválido pelo serviço externo.");
    }
}
