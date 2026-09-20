package br.com.desafiovotacao.controller;

import br.com.desafiovotacao.dto.RegistrarVotoRequest;
import br.com.desafiovotacao.dto.VotoResponse;
import br.com.desafiovotacao.service.VotoService;
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
@RequestMapping("/api/v1/pautas/{pautaId}/votos")
@RequiredArgsConstructor
public class VotoController {

    private final VotoService votoService;

    @PostMapping
    public ResponseEntity<VotoResponse> registrar(@PathVariable Long pautaId,
                                                  @Valid @RequestBody RegistrarVotoRequest request) {
        VotoResponse response = votoService.registrar(pautaId, request);
        URI location = URI.create("/api/v1/pautas/" + pautaId + "/votos/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
}
