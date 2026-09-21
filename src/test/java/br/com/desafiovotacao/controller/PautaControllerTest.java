package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.exception.GlobalExceptionHandler;
import java.time.Clock;
import br.com.desafiovotacao.dto.CriarPautaRequest;
import br.com.desafiovotacao.dto.PautaResponse;
import br.com.desafiovotacao.service.PautaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PautaControllerTest {

    @Mock
    private PautaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PautaController(service))
                .setControllerAdvice(new GlobalExceptionHandler(Clock.systemDefaultZone())).build();
    }

    @Test
    void deveRetornarCreatedComLocationEDadosDaPauta() throws Exception {
        CriarPautaRequest request = new CriarPautaRequest("Aquisição de veículos", "Compra de dois veículos.");
        LocalDateTime dataCriacao = LocalDateTime.of(2026, 9, 19, 10, 30);
        when(service.criar(request)).thenReturn(new PautaResponse(1L, request.titulo(), request.descricao(), dataCriacao));

        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Aquisição de veículos","descricao":"Compra de dois veículos."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pautas/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value(request.titulo()))
                .andExpect(jsonPath("$.descricao").value(request.descricao()))
                .andExpect(jsonPath("$.dataCriacao").value("2026-09-19T10:30:00"));

        verify(service).criar(request);
    }

    @ParameterizedTest
    @MethodSource("requestsInvalidos")
    void deveRetornarBadRequestSemChamarServico(String json) throws Exception {
        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    static Stream<String> requestsInvalidos() {
        return Stream.of(
                "{\"titulo\":\"\",\"descricao\":\"Descrição\"}",
                "{\"titulo\":\"   \",\"descricao\":\"Descrição\"}",
                "{\"descricao\":\"Descrição\"}",
                "{\"titulo\":\"" + "a".repeat(151) + "\",\"descricao\":\"Descrição\"}",
                "{\"titulo\":\"Título\",\"descricao\":\"\"}",
                "{\"titulo\":\"Título\",\"descricao\":\"   \"}",
                "{\"titulo\":\"Título\"}",
                "{\"titulo\":\"Título\",\"descricao\":\"" + "a".repeat(1001) + "\"}"
        );
    }
}
