package br.com.desafiovotacao.service;

import br.com.desafiovotacao.dto.AbrirSessaoRequest;
import br.com.desafiovotacao.dto.SessaoVotacaoResponse;

public interface SessaoVotacaoService {

    SessaoVotacaoResponse abrir(Long pautaId, AbrirSessaoRequest request);
}
