package br.com.desafiovotacao.service;

import br.com.desafiovotacao.dto.ContabilizacaoVotosResponse;
import br.com.desafiovotacao.dto.RegistrarVotoRequest;
import br.com.desafiovotacao.dto.VotoResponse;

public interface VotoService {

    VotoResponse registrar(Long pautaId, RegistrarVotoRequest request);

    ContabilizacaoVotosResponse contabilizar(Long pautaId);
}
