package br.com.desafiovotacao.config;

import br.com.desafiovotacao.controller.*;
import br.com.desafiovotacao.exception.GlobalExceptionHandler;
import br.com.desafiovotacao.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.Clock;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(OpenApiTest.Config.class)
@WebAppConfiguration
class OpenApiTest {
    @Configuration
    @EnableAutoConfiguration(excludeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
    @Import({OpenApiConfig.class, PautaController.class, SessaoVotacaoController.class,
            VotoController.class, ResultadoController.class, MobileController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean PautaService pautaService() { return mock(PautaService.class); }
        @Bean SessaoVotacaoService sessaoService() { return mock(SessaoVotacaoService.class); }
        @Bean VotoService votoService() { return mock(VotoService.class); }
        @Bean Clock clock() { return Clock.systemUTC(); }
    }
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    @BeforeEach void configurar() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test void deveGerarContratoRealComErrosESchemas() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Desafio Votação API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths.*").value(org.hamcrest.Matchers.hasSize(7)))
                .andExpect(jsonPath("$.paths['/api/v1/pautas'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pautas/{pautaId}/votos'].post.responses['403'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErroResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/pautas/{pautaId}/votos'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pautas/{pautaId}/votos'].post.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.RegistrarVotoRequest.properties.associadoId").exists())
                .andExpect(jsonPath("$.components.schemas.ContabilizacaoVotosResponse.properties.sim").exists())
                .andExpect(jsonPath("$.components.schemas.ErroResponse.properties.message").exists());
    }
    @Test void deveDisponibilizarSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
