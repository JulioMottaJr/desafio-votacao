package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MobileControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.standaloneSetup(new MobileController())
                .setControllerAdvice(new GlobalExceptionHandler(Clock.systemDefaultZone())).build();
    }

    @Test
    void deveDescreverFormularioDePauta() throws Exception {
        mvc.perform(get("/api/v1/mobile/pautas/nova"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "tipo": "FORMULARIO",
                          "titulo": "Cadastrar pauta",
                          "campos": [
                            {"nome":"titulo","label":"Título","tipo":"TEXTO","obrigatorio":true},
                            {"nome":"descricao","label":"Descrição","tipo":"TEXTO","obrigatorio":true}
                          ],
                          "acoes": [
                            {"label":"Cadastrar","metodo":"POST","url":"/api/v1/pautas","body":{}}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveDescreverFormularioDeSessaoComDuracaoOpcional() throws Exception {
        mvc.perform(get("/api/v1/mobile/pautas/42/sessoes/nova"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "tipo": "FORMULARIO",
                          "titulo": "Abrir sessão de votação",
                          "campos": [
                            {"nome":"duracaoMinutos","label":"Duração em minutos (opcional)",
                             "tipo":"INTEIRO","obrigatorio":false}
                          ],
                          "acoes": [
                            {"label":"Abrir sessão","metodo":"POST","url":"/api/v1/pautas/42/sessoes","body":{}}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void deveDescreverSelecaoComBodiesCompativeisComRegistroDeVoto() throws Exception {
        mvc.perform(get("/api/v1/mobile/pautas/42/votos/novo"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "tipo": "SELECAO",
                          "titulo": "Votar",
                          "campos": [
                            {"nome":"associadoId","label":"Identificador do associado",
                             "tipo":"TEXTO","obrigatorio":true}
                          ],
                          "opcoes": [
                            {"valor":"SIM","label":"Sim",
                             "acao":{"label":"Votar Sim","metodo":"POST",
                                     "url":"/api/v1/pautas/42/votos","body":{"opcao":"SIM"}}},
                            {"valor":"NAO","label":"Não",
                             "acao":{"label":"Votar Não","metodo":"POST",
                                     "url":"/api/v1/pautas/42/votos","body":{"opcao":"NAO"}}}
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/mobile/pautas/abc/sessoes/nova",
            "/api/v1/mobile/pautas/abc/votos/novo"})
    void deveRejeitarPautaIdIncompativelComLong(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(url));
    }
}
