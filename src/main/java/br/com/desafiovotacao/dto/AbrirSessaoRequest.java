package br.com.desafiovotacao.dto;

import jakarta.validation.constraints.Min;

public record AbrirSessaoRequest(@Min(1) Integer duracaoMinutos) {
}
