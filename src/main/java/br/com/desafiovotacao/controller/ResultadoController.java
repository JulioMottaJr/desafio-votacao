package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.dto.ContabilizacaoVotosResponse;
import br.com.desafiovotacao.service.VotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/resultado")
@RequiredArgsConstructor
public class ResultadoController {

    private final VotoService votoService;

    @GetMapping
    public ResponseEntity<ContabilizacaoVotosResponse> consultar(@PathVariable Long pautaId) {
        return ResponseEntity.ok(votoService.contabilizar(pautaId));
    }
}
