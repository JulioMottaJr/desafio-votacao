package br.com.desafiovotacao.dto;

import br.com.desafiovotacao.entity.OpcaoVoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarVotoRequest(
        @NotBlank @Size(max = 100) String associadoId,
        @NotNull OpcaoVoto opcao
) {
}
