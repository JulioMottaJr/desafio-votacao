package br.com.desafiovotacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 1000) String descricao
) {
}
