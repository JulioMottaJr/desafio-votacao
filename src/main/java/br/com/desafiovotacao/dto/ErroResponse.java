package br.com.desafiovotacao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> fields
) {
}
