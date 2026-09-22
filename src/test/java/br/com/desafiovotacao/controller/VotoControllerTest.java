package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.client.CpfInvalidoException;
import br.com.desafiovotacao.exception.AssociadoNaoHabilitadoException;

import br.com.desafiovotacao.exception.GlobalExceptionHandler;
import java.time.Clock;
import br.com.desafiovotacao.dto.RegistrarVotoRequest;
import br.com.desafiovotacao.dto.VotoResponse;
import br.com.desafiovotacao.entity.OpcaoVoto;
import br.com.desafiovotacao.exception.PautaNaoEncontradaException;
import br.com.desafiovotacao.exception.SessaoNaoEncontradaException;
import br.com.desafiovotacao.exception.SessaoEncerradaException;
import br.com.desafiovotacao.exception.VotoDuplicadoException;
import br.com.desafiovotacao.service.VotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VotoControllerTest {

    @Mock
    private VotoService service;
    private MockMvc mockMvc;
    private final LocalDateTime dataVoto = LocalDateTime.of(2026, 9, 20, 10, 0);

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VotoController(service))
                .setControllerAdvice(new GlobalExceptionHandler(Clock.systemDefaultZone())).build();
    }

    @ParameterizedTest
    @EnumSource(OpcaoVoto.class)
    void deveRetornarCreatedComLocationEDadosDoVoto(OpcaoVoto opcao) throws Exception {
        RegistrarVotoRequest request = new RegistrarVotoRequest("00123456", opcao);
        when(service.registrar(1L, request)).thenReturn(new VotoResponse(3L, 1L, "00123456", opcao, dataVoto));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"00123456\",\"opcao\":\"" + opcao + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pautas/1/votos/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.pautaId").value(1))
                .andExpect(jsonPath("$.associadoId").value("00123456"))
                .andExpect(jsonPath("$.opcao").value(opcao.name()))
                .andExpect(jsonPath("$.dataVoto").value("2026-09-20T10:00:00"))
                .andExpect(jsonPath("$.pauta").doesNotExist())
                .andExpect(jsonPath("$.sessaoVotacao").doesNotExist());

        verify(service).registrar(1L, request);
    }

    @ParameterizedTest
    @MethodSource("requestsInvalidos")
    void deveRejeitarRequestInvalido(String body) throws Exception {
        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    static Stream<String> requestsInvalidos() {
        return Stream.of(
                "{\"associadoId\":\"\",\"opcao\":\"SIM\"}",
                "{\"associadoId\":\"   \",\"opcao\":\"SIM\"}",
                "{\"opcao\":\"SIM\"}",
                "{\"associadoId\":null,\"opcao\":\"SIM\"}",
                "{\"associadoId\":\"" + "a".repeat(101) + "\",\"opcao\":\"SIM\"}",
                "{\"associadoId\":\"123\"}",
                "{\"associadoId\":\"123\",\"opcao\":null}",
                "{\"associadoId\":\"123\",\"opcao\":\"TALVEZ\"}",
                "{\"associadoId\":\"123\",\"opcao\":\"sim\"}",
                "{\"associadoId\":\"123\",\"opcao\":\" SIM \"}",
                "{\"associadoId\":\"123\",\"opcao\":0}",
                "{\"associadoId\":\"123\",\"opcao\":\"0\"}",
                "{\"associadoId\":\"123\",\"opcao\":true}"
        );
    }

    @ParameterizedTest
    @MethodSource("errosDeDominio")
    void deveRetornarStatusDaExcecaoDeDominio(RuntimeException exception, int statusEsperado) throws Exception {
        RegistrarVotoRequest request = new RegistrarVotoRequest("123", OpcaoVoto.SIM);
        when(service.registrar(1L, request)).thenThrow(exception);

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"123\",\"opcao\":\"SIM\"}"))
                .andExpect(status().is(statusEsperado));
    }

    @ParameterizedTest
    @MethodSource("errosDeElegibilidade")
    void deveRetornarErroDeElegibilidadePadronizadoSemExporCpf(RuntimeException exception,
            int statusEsperado, String error) throws Exception {
        RegistrarVotoRequest request = new RegistrarVotoRequest("00000000000", OpcaoVoto.SIM);
        when(service.registrar(1L, request)).thenThrow(exception);

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"00000000000\",\"opcao\":\"SIM\"}"))
                .andExpect(status().is(statusEsperado))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(statusEsperado))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(exception.getMessage()))
                .andExpect(jsonPath("$.path").value("/api/v1/pautas/1/votos"))
                .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(5)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(request.associadoId()))))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    static Stream<Arguments> errosDeElegibilidade() {
        return Stream.of(
                Arguments.of(new CpfInvalidoException(), 404, "Not Found"),
                Arguments.of(new AssociadoNaoHabilitadoException(), 403, "Forbidden")
        );
    }

    static Stream<Arguments> errosDeDominio() {
        return Stream.of(
                Arguments.of(new PautaNaoEncontradaException(1L), 404),
                Arguments.of(new SessaoNaoEncontradaException(1L), 404),
                Arguments.of(new SessaoEncerradaException(1L), 409),
                Arguments.of(new VotoDuplicadoException(1L), 409)
        );
    }
}
