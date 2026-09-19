package br.com.desafiovotacao.service.impl;

import br.com.desafiovotacao.dto.CriarPautaRequest;
import br.com.desafiovotacao.dto.PautaResponse;
import br.com.desafiovotacao.entity.Pauta;
import br.com.desafiovotacao.repository.PautaRepository;
import br.com.desafiovotacao.service.PautaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PautaServiceImpl implements PautaService {

    private final PautaRepository pautaRepository;

    @Override
    @Transactional
    public PautaResponse criar(CriarPautaRequest request) {
        log.info("Criando nova pauta. titulo={}", request.titulo());
        Pauta pauta = new Pauta(request.titulo(), request.descricao(), LocalDateTime.now());
        Pauta salva = pautaRepository.save(pauta);
        log.info("Pauta criada com sucesso. pautaId={}", salva.getId());
        return new PautaResponse(salva.getId(), salva.getTitulo(), salva.getDescricao(), salva.getDataCriacao());
    }
}
