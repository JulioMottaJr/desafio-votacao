package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.exception.GlobalExceptionHandler;
import java.time.Clock;
import br.com.desafiovotacao.dto.ContabilizacaoVotosResponse;
import br.com.desafiovotacao.exception.PautaNaoEncontradaException;
import br.com.desafiovotacao.service.VotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResultadoControllerTest {

    @Mock
    private VotoService service;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ResultadoController(service))
                .setControllerAdvice(new GlobalExceptionHandler(Clock.systemDefaultZone())).build();
    }

    @ParameterizedTest
    @CsvSource({"1, 150, 37, 187", "2, 0, 0, 0"})
    void deveRetornarResultadoDoServicoIncluindoPautaSemVotos(
            long pautaId, long sim, long nao, long total) throws Exception {
        when(service.contabilizar(pautaId))
                .thenReturn(new ContabilizacaoVotosResponse(pautaId, sim, nao, total));

        mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pautaId").value(pautaId))
                .andExpect(jsonPath("$.sim").value(sim))
                .andExpect(jsonPath("$.nao").value(nao))
                .andExpect(jsonPath("$.total").value(total))
                .andExpect(jsonPath("$.length()").value(4));

        verify(service).contabilizar(pautaId);
        verifyNoMoreInteractions(service);
    }

    @Test
    void deveRetornarNotFoundParaPautaInexistente() throws Exception {
        when(service.contabilizar(99L)).thenThrow(new PautaNaoEncontradaException(99L));

        mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", 99L))
                .andExpect(status().isNotFound());

        verify(service).contabilizar(99L);
        verifyNoMoreInteractions(service);
    }
}
