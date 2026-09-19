package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.dto.CriarPautaRequest;
import br.com.desafiovotacao.dto.PautaResponse;
import br.com.desafiovotacao.service.PautaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/pautas")
@RequiredArgsConstructor
public class PautaController {

    private final PautaService pautaService;

    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody CriarPautaRequest request) {
        PautaResponse response = pautaService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/pautas/" + response.id())).body(response);
    }
}
