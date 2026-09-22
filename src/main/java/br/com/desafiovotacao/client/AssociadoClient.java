package br.com.desafiovotacao.client;

public interface AssociadoClient {

    /**
     * Consulta a elegibilidade do CPF no sistema externo.
     *
     * @return elegibilidade de um CPF considerado válido; {@code UNABLE_TO_VOTE}
     *         indica inaptidão para votar, não CPF inválido
     * @throws CpfInvalidoException quando o sistema externo considera o CPF inválido
     *         ou não encontrado, representando seu HTTP 404 (Not Found), sem retorno
     *         de uma resposta de elegibilidade
     */
    ElegibilidadeResponse consultarElegibilidade(String cpf);
}
