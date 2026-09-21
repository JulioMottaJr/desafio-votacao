package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.dto.mobile.TelaResponse;
import br.com.desafiovotacao.dto.mobile.TelaResponse.Acao;
import br.com.desafiovotacao.dto.mobile.TelaResponse.Campo;
import br.com.desafiovotacao.dto.mobile.TelaResponse.Opcao;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static br.com.desafiovotacao.dto.mobile.TelaResponse.TipoCampo.INTEIRO;
import static br.com.desafiovotacao.dto.mobile.TelaResponse.TipoCampo.TEXTO;
import static br.com.desafiovotacao.dto.mobile.TelaResponse.TipoTela.FORMULARIO;
import static br.com.desafiovotacao.dto.mobile.TelaResponse.TipoTela.SELECAO;

@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {

    @GetMapping("/pautas/nova")
    public TelaResponse novaPauta() {
        return new TelaResponse(FORMULARIO, "Cadastrar pauta",
                List.of(new Campo("titulo", "Título", TEXTO, true),
                        new Campo("descricao", "Descrição", TEXTO, true)),
                List.of(new Acao("Cadastrar", "POST", "/api/v1/pautas", Map.of())),
                List.of());
    }

    @GetMapping("/pautas/{pautaId}/sessoes/nova")
    public TelaResponse novaSessao(@PathVariable Long pautaId) {
        return new TelaResponse(FORMULARIO, "Abrir sessão de votação",
                List.of(new Campo("duracaoMinutos", "Duração em minutos (opcional)", INTEIRO, false)),
                List.of(new Acao("Abrir sessão", "POST", "/api/v1/pautas/" + pautaId + "/sessoes", Map.of())),
                List.of());
    }

    @GetMapping("/pautas/{pautaId}/votos/novo")
    public TelaResponse novoVoto(@PathVariable Long pautaId) {
        String url = "/api/v1/pautas/" + pautaId + "/votos";
        return new TelaResponse(SELECAO, "Votar",
                List.of(new Campo("associadoId", "Identificador do associado", TEXTO, true)),
                List.of(),
                List.of(new Opcao("SIM", "Sim", new Acao("Votar Sim", "POST", url, Map.of("opcao", "SIM"))),
                        new Opcao("NAO", "Não", new Acao("Votar Não", "POST", url, Map.of("opcao", "NAO")))));
    }
}
