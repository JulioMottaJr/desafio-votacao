package br.com.desafiovotacao.config;

import br.com.desafiovotacao.dto.ErroResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI votacaoOpenApi() {
        return new OpenAPI().info(new Info().title("Desafio Votação API").version("v1")
                .description("API REST para gerenciamento de pautas, sessões de votação, votos e contabilização de resultados."));
    }

    @Bean
    OpenApiCustomizer documentarOperacoes() {
        return api -> {
            ModelConverters.getInstance().read(ErroResponse.class).forEach(api.getComponents()::addSchemas);
            api.getPaths().forEach((rota, item) -> item.readOperations().forEach(op -> {
                // As rotas e os schemas de sucesso continuam derivados dos controllers/DTOs reais.
                if (!rota.startsWith("/api/v1/")) return;
                op.getResponses().entrySet().removeIf(e -> !e.getKey().startsWith("2"));
                if (!rota.startsWith("/api/v1/mobile/") && !rota.endsWith("/resultado")) {
                    ApiResponse sucesso = op.getResponses().remove("200");
                    if (sucesso != null) {
                        sucesso.setDescription("Recurso criado com sucesso.");
                        sucesso.addHeaderObject("Location", new io.swagger.v3.oas.models.headers.Header()
                                .description("URI do recurso criado; não implica existência de endpoint GET individual.")
                                .schema(new io.swagger.v3.oas.models.media.StringSchema()));
                        op.getResponses().addApiResponse("201", sucesso);
                    }
                }
                op.getResponses().addApiResponse("400", erro("Payload, JSON ou parâmetro inválido."));
                op.getResponses().addApiResponse("500", erro("Falha inesperada; resposta sem detalhes internos."));
                if (op.getParameters() != null) op.getParameters().forEach(p -> {
                    if ("pautaId".equals(p.getName())) p.setDescription("Identificador da pauta.");
                });
                if (rota.startsWith("/api/v1/mobile/")) {
                    op.setSummary(rota.endsWith("/votos/novo") ? "Obter tela SELECAO para votar"
                            : rota.endsWith("/sessoes/nova") ? "Obter FORMULARIO de abertura de sessão"
                            : "Obter FORMULARIO de cadastro de pauta");
                    op.setDescription("Retorna o contrato JSON da tela e ações POST. Não consulta existência da pauta.");
                } else if (rota.endsWith("/resultado")) {
                    op.setSummary("Contabilizar votos da pauta");
                    op.setDescription("Retorna sim, nao e total, inclusive antes do encerramento; pauta sem votos retorna zeros.");
                    op.getResponses().addApiResponse("404", erro("Pauta não encontrada."));
                } else if (rota.endsWith("/votos")) {
                    op.setSummary("Registrar voto SIM ou NAO");
                    op.setDescription("associadoId é consultado como CPF no Fake Client aleatório. ABLE_TO_VOTE permite registrar; cada associado vota uma vez por pauta.");
                    op.getResponses().addApiResponse("403", erro("CPF válido, mas UNABLE_TO_VOTE: associado não habilitado."));
                    op.getResponses().addApiResponse("404", erro("Pauta ou sessão não encontrada, ou CPF considerado inválido pelo Fake Client."));
                    op.getResponses().addApiResponse("409", erro("Voto duplicado ou sessão encerrada."));
                } else if (rota.endsWith("/sessoes")) {
                    op.setSummary("Abrir sessão de votação");
                    op.setDescription("Envie {} para duração padrão de 1 minuto ou duracaoMinutos inteiro >= 1. Apenas uma sessão por pauta.");
                    op.getResponses().addApiResponse("404", erro("Pauta não encontrada."));
                    op.getResponses().addApiResponse("409", erro("Sessão já existente para a pauta."));
                } else {
                    op.setSummary("Cadastrar pauta");
                    op.setDescription("Título e descrição obrigatórios, com até 150 e 1000 caracteres, respectivamente.");
                }
            }));
        };
    }

    private ApiResponse erro(String descricao) {
        return new ApiResponse().description(descricao).content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroResponse"))));
    }
}
