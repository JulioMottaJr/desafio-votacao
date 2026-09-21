package br.com.desafiovotacao.dto.mobile;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * O cliente resolve as URLs relativas contra a origem configurada da API.
 * Ao acionar um botão/opção, copia o body base e acrescenta os valores dos campos
 * usando seus nomes como chaves. Campos opcionais não preenchidos são omitidos.
 * TEXTO produz String e INTEIRO produz número JSON. SELECAO também pode coletar
 * campos comuns às opções, como associadoId. As validações finais pertencem à API.
 */
public record TelaResponse(
        TipoTela tipo,
        String titulo,
        List<Campo> campos,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Acao> acoes,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Opcao> opcoes
) {
    public enum TipoTela { FORMULARIO, SELECAO }

    public enum TipoCampo { TEXTO, INTEIRO }

    public record Campo(String nome, String label, TipoCampo tipo, boolean obrigatorio) {
    }

    public record Acao(String label, String metodo, String url, Map<String, Object> body) {
    }

    public record Opcao(String valor, String label, Acao acao) {
    }
}
