package br.com.desafiovotacao.client;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FakeAssociadoClientTest {

    private static final String CPF_FICTICIO = "00000000000";

    private final BooleanSupplier decisaoAleatoria = mock(BooleanSupplier.class);
    private final AssociadoClient client = new FakeAssociadoClient(decisaoAleatoria);

    @Test
    void deveRetornarAbleToVoteParaCpfConsideradoValidoEApto() {
        when(decisaoAleatoria.getAsBoolean()).thenReturn(true, true);

        ElegibilidadeResponse response = assertDoesNotThrow(
                () -> client.consultarElegibilidade(CPF_FICTICIO));

        assertThat(response.status()).isEqualTo(StatusElegibilidade.ABLE_TO_VOTE);
        verify(decisaoAleatoria, times(2)).getAsBoolean();
        verifyNoMoreInteractions(decisaoAleatoria);
    }

    @Test
    void deveRetornarUnableToVoteParaCpfConsideradoValidoEInapto() {
        when(decisaoAleatoria.getAsBoolean()).thenReturn(true, false);

        ElegibilidadeResponse response = assertDoesNotThrow(
                () -> client.consultarElegibilidade(CPF_FICTICIO));

        assertThat(response.status()).isEqualTo(StatusElegibilidade.UNABLE_TO_VOTE);
        verify(decisaoAleatoria, times(2)).getAsBoolean();
        verifyNoMoreInteractions(decisaoAleatoria);
    }

    @Test
    void deveRejeitarCpfConsideradoInvalidoSemConsultarElegibilidadeOuExporCpf() {
        when(decisaoAleatoria.getAsBoolean()).thenReturn(false);

        assertThatThrownBy(() -> client.consultarElegibilidade(CPF_FICTICIO))
                .isExactlyInstanceOf(CpfInvalidoException.class)
                .hasMessage("CPF considerado inválido pelo serviço externo.")
                .hasMessageNotContaining(CPF_FICTICIO)
                .hasNoCause();
        verify(decisaoAleatoria).getAsBoolean();
        verifyNoMoreInteractions(decisaoAleatoria);
    }

    @Test
    void devePermitirResultadosDiferentesParaOMesmoCpfSemCache() {
        when(decisaoAleatoria.getAsBoolean()).thenReturn(true, true, true, false, false);

        assertThat(client.consultarElegibilidade(CPF_FICTICIO).status())
                .isEqualTo(StatusElegibilidade.ABLE_TO_VOTE);
        assertThat(client.consultarElegibilidade(CPF_FICTICIO).status())
                .isEqualTo(StatusElegibilidade.UNABLE_TO_VOTE);
        assertThatThrownBy(() -> client.consultarElegibilidade(CPF_FICTICIO))
                .isInstanceOf(CpfInvalidoException.class);
        verify(decisaoAleatoria, times(5)).getAsBoolean();
    }

    @Test
    void deveDisponibilizarFakePelaInterfaceNoSpring() {
        try (var context = new AnnotationConfigApplicationContext(FakeAssociadoClient.class)) {
            assertThat(context.getBean(AssociadoClient.class))
                    .isInstanceOf(FakeAssociadoClient.class);
        }
    }
}
