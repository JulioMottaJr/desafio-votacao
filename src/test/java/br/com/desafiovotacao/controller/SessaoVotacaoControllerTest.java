package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.dto.AbrirSessaoRequest;
import br.com.desafiovotacao.dto.SessaoVotacaoResponse;
import br.com.desafiovotacao.exception.PautaNaoEncontradaException;
import br.com.desafiovotacao.exception.SessaoJaExistenteException;
import br.com.desafiovotacao.service.SessaoVotacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SessaoVotacaoControllerTest {

    @Mock
    private SessaoVotacaoService service;
    private MockMvc mockMvc;
    private final LocalDateTime abertura = LocalDateTime.of(2026, 9, 19, 17, 0);

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SessaoVotacaoController(service)).build();
    }

    @Test
    void deveRetornarCreatedComLocationEDadosDaSessao() throws Exception {
        AbrirSessaoRequest request = new AbrirSessaoRequest(5);
        when(service.abrir(1L, request))
                .thenReturn(new SessaoVotacaoResponse(2L, 1L, abertura, abertura.plusMinutes(5)));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"duracaoMinutos\":5}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pautas/1/sessoes/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.pautaId").value(1))
                .andExpect(jsonPath("$.dataAbertura").value("2026-09-19T17:00:00"))
                .andExpect(jsonPath("$.dataEncerramento").value("2026-09-19T17:05:00"))
                .andExpect(jsonPath("$.pauta").doesNotExist());

        verify(service).abrir(1L, request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"duracaoMinutos\":null}"})
    void deveAceitarDuracaoNaoInformada(String body) throws Exception {
        AbrirSessaoRequest request = new AbrirSessaoRequest(null);
        when(service.abrir(1L, request))
                .thenReturn(new SessaoVotacaoResponse(2L, 1L, abertura, abertura.plusMinutes(1)));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataEncerramento").value("2026-09-19T17:01:00"));

        verify(service).abrir(1L, request);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveRejeitarDuracaoInvalida(int duracao) throws Exception {
        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoMinutos\":" + duracao + "}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarNotFoundParaPautaInexistente() throws Exception {
        when(service.abrir(1L, new AbrirSessaoRequest(null))).thenThrow(new PautaNaoEncontradaException(1L));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornarConflictParaSessaoJaExistente() throws Exception {
        when(service.abrir(1L, new AbrirSessaoRequest(null))).thenThrow(new SessaoJaExistenteException(1L));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }
}
