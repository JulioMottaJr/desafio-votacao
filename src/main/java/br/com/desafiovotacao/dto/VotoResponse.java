package br.com.desafiovotacao.dto;

import br.com.desafiovotacao.entity.OpcaoVoto;

import java.time.LocalDateTime;

public record VotoResponse(Long id, Long pautaId, String associadoId, OpcaoVoto opcao,
                           LocalDateTime dataVoto) {
}
