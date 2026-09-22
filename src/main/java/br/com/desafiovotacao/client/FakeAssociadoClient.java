package br.com.desafiovotacao.client;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

@Component
public class FakeAssociadoClient implements AssociadoClient {

    private final BooleanSupplier decisaoAleatoria;

    public FakeAssociadoClient() {
        this(() -> ThreadLocalRandom.current().nextBoolean());
    }

    FakeAssociadoClient(BooleanSupplier decisaoAleatoria) {
        this.decisaoAleatoria = decisaoAleatoria;
    }

    @Override
    public ElegibilidadeResponse consultarElegibilidade(String cpf) {
        // O enunciado pede validade simulada, sem cálculo ou cache por CPF.
        boolean cpfValido = decisaoAleatoria.getAsBoolean();
        if (!cpfValido) {
            throw new CpfInvalidoException();
        }

        boolean podeVotar = decisaoAleatoria.getAsBoolean();
        return new ElegibilidadeResponse(podeVotar
                ? StatusElegibilidade.ABLE_TO_VOTE
                : StatusElegibilidade.UNABLE_TO_VOTE);
    }
}
