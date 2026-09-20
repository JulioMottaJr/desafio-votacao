package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.dto.AbrirSessaoRequest;
import br.com.desafiovotacao.dto.SessaoVotacaoResponse;
import br.com.desafiovotacao.service.SessaoVotacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessoes")
@RequiredArgsConstructor
public class SessaoVotacaoController {

    private final SessaoVotacaoService sessaoVotacaoService;

    @PostMapping
    public ResponseEntity<SessaoVotacaoResponse> abrir(@PathVariable Long pautaId,
                                                     @Valid @RequestBody AbrirSessaoRequest request) {
        SessaoVotacaoResponse response = sessaoVotacaoService.abrir(pautaId, request);
        URI location = URI.create("/api/v1/pautas/" + pautaId + "/sessoes/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
}
