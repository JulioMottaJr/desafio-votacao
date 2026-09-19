package br.com.desafiovotacao.service;

import br.com.desafiovotacao.dto.CriarPautaRequest;
import br.com.desafiovotacao.dto.PautaResponse;

public interface PautaService {

    PautaResponse criar(CriarPautaRequest request);
}
