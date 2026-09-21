package br.com.desafiovotacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContabilizacaoVotosResponse(
        Long pautaId,
        @JsonProperty("sim") long votosSim,
        @JsonProperty("nao") long votosNao,
        @JsonProperty("total") long totalVotos
) {
}
