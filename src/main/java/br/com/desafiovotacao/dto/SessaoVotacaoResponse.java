package br.com.desafiovotacao.dto;

import java.time.LocalDateTime;

public record SessaoVotacaoResponse(Long id, Long pautaId, LocalDateTime dataAbertura,
                                  LocalDateTime dataEncerramento) {
}
