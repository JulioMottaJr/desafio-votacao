package br.com.desafiovotacao.exception;

import br.com.desafiovotacao.controller.PautaController;
import br.com.desafiovotacao.controller.ResultadoController;
import br.com.desafiovotacao.controller.SessaoVotacaoController;
import br.com.desafiovotacao.controller.VotoController;
import br.com.desafiovotacao.service.PautaService;
import br.com.desafiovotacao.service.SessaoVotacaoService;
import br.com.desafiovotacao.service.VotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private PautaService pautaService;
    @Mock private VotoService votoService;
    @Mock private SessaoVotacaoService sessaoService;
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
        mvc = MockMvcBuilders.standaloneSetup(new PautaController(pautaService),
                        new ResultadoController(votoService), new VotoController(votoService),
                        new SessaoVotacaoController(sessaoService))
                .setControllerAdvice(new GlobalExceptionHandler(clock)).build();
    }

    @ParameterizedTest
    @MethodSource("excecoesDeDominio")
    void devePreservarStatusEMensagemDeDominio(RuntimeException exception, int codigo) throws Exception {
        when(votoService.contabilizar(1L)).thenThrow(exception);
        verificarContrato(mvc.perform(get("/api/v1/pautas/1/resultado")), codigo,
                exception.getMessage(), "/api/v1/pautas/1/resultado")
                .andExpect(jsonPath("$.fields").doesNotExist());
    }

    static Stream<Arguments> excecoesDeDominio() {
        return Stream.of(
                Arguments.of(new PautaNaoEncontradaException(1L), 404),
                Arguments.of(new SessaoNaoEncontradaException(1L), 404),
                Arguments.of(new SessaoJaExistenteException(1L), 409),
                Arguments.of(new SessaoEncerradaException(1L), 409),
                Arguments.of(new VotoDuplicadoException(1L), 409));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"titulo\":\"\",\"descricao\":\"   \"}"})
    void deveInformarTodosOsCamposInvalidos(String body) throws Exception {
        verificarContrato(mvc.perform(post("/api/v1/pautas").contentType(MediaType.APPLICATION_JSON)
                        .content(body)), 400, "Erro de validação", "/api/v1/pautas")
                .andExpect(jsonPath("$.fields.titulo").isNotEmpty())
                .andExpect(jsonPath("$.fields.descricao").isNotEmpty())
                .andExpect(jsonPath("$.fields.length()").value(2));
        verifyNoInteractions(pautaService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"associadoId\":\"123\",\"opcao\":\"TALVEZ\"}"})
    void deveRejeitarJsonOuEnumInvalidoSemExporDetalhes(String body) throws Exception {
        verificarContrato(mvc.perform(post("/api/v1/pautas/1/votos").contentType(MediaType.APPLICATION_JSON)
                        .content(body)), 400, "Corpo da requisição inválido", "/api/v1/pautas/1/votos")
                .andExpect(jsonPath("$.length()").value(5));
        verifyNoInteractions(votoService);
    }

    @Test
    void deveValidarDuracao() throws Exception {
        verificarContrato(mvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"duracaoMinutos\":0}")),
                400, "Erro de validação", "/api/v1/pautas/1/sessoes")
                .andExpect(jsonPath("$.fields.duracaoMinutos").isNotEmpty());
        verifyNoInteractions(sessaoService);
    }

    @Test
    void deveRejeitarParametroInvalido() throws Exception {
        verificarContrato(mvc.perform(get("/api/v1/pautas/abc/resultado")),
                400, "Parâmetros da requisição inválidos", "/api/v1/pautas/abc/resultado");
        verifyNoInteractions(votoService);
    }

    @Test
    void deveOcultarDetalhesDoErroInesperado() throws Exception {
        when(votoService.contabilizar(1L)).thenThrow(new IllegalStateException("Detalhe interno de diagnóstico"));
        verificarContrato(mvc.perform(get("/api/v1/pautas/1/resultado")),
                500, "Ocorreu um erro interno inesperado.", "/api/v1/pautas/1/resultado")
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void naoDeveConverterMetodoNaoPermitidoEmErro500() throws Exception {
        verificarContrato(mvc.perform(post("/api/v1/pautas/1/resultado")),
                405, "Method Not Allowed", "/api/v1/pautas/1/resultado")
                .andExpect(header().exists("Allow"));
        verifyNoInteractions(votoService);
    }

    private ResultActions verificarContrato(ResultActions result, int codigo, String mensagem, String path)
            throws Exception {
        return result.andExpect(status().is(codigo))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").value("2026-09-21T10:00:00"))
                .andExpect(jsonPath("$.status").value(codigo))
                .andExpect(jsonPath("$.error").value(HttpStatus.valueOf(codigo).getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(mensagem))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
