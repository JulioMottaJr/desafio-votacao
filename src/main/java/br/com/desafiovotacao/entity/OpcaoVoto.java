package br.com.desafiovotacao.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OpcaoVoto {
    SIM,
    NAO;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OpcaoVoto fromJson(Object valor) {
        if ("SIM".equals(valor)) {
            return SIM;
        }
        if ("NAO".equals(valor)) {
            return NAO;
        }
        throw new IllegalArgumentException("Opção de voto deve ser SIM ou NAO.");
    }
}
