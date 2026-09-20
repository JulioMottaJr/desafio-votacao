package br.com.desafiovotacao.service.impl;

import br.com.desafiovotacao.dto.AbrirSessaoRequest;
import br.com.desafiovotacao.dto.SessaoVotacaoResponse;
import br.com.desafiovotacao.entity.Pauta;
import br.com.desafiovotacao.entity.SessaoVotacao;
import br.com.desafiovotacao.exception.PautaNaoEncontradaException;
import br.com.desafiovotacao.exception.SessaoJaExistenteException;
import br.com.desafiovotacao.repository.PautaRepository;
import br.com.desafiovotacao.repository.SessaoVotacaoRepository;
import br.com.desafiovotacao.service.SessaoVotacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessaoVotacaoServiceImpl implements SessaoVotacaoService {

    private final PautaRepository pautaRepository;
    private final SessaoVotacaoRepository sessaoVotacaoRepository;
    private final Clock clock;

    @Override
    @Transactional
    public SessaoVotacaoResponse abrir(Long pautaId, AbrirSessaoRequest request) {
        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        if (sessaoVotacaoRepository.existsByPautaId(pautaId)) {
            throw sessaoJaExistente(pautaId);
        }

        int duracao = request.duracaoMinutos() == null ? 1 : request.duracaoMinutos();
        LocalDateTime dataAbertura = LocalDateTime.now(clock);
        SessaoVotacao sessao = new SessaoVotacao(pauta, dataAbertura, dataAbertura.plusMinutes(duracao));
        SessaoVotacao salva;
        try {
            // O flush permite identificar a constraint antes de sair do método transacional.
            salva = sessaoVotacaoRepository.saveAndFlush(sessao);
        } catch (DataIntegrityViolationException exception) {
            if (violouUnicidadeDaPauta(exception)) {
                throw sessaoJaExistente(pautaId);
            }
            throw exception;
        }

        log.info("Sessão de votação aberta. pautaId={}, sessaoId={}, dataEncerramento={}",
                pautaId, salva.getId(), salva.getDataEncerramento());
        return new SessaoVotacaoResponse(salva.getId(), salva.getPauta().getId(),
                salva.getDataAbertura(), salva.getDataEncerramento());
    }

    private SessaoJaExistenteException sessaoJaExistente(Long pautaId) {
        log.warn("Tentativa de abertura de sessão para pauta que já possui sessão. pautaId={}", pautaId);
        return new SessaoJaExistenteException(pautaId);
    }

    private boolean violouUnicidadeDaPauta(Throwable exception) {
        for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && "uk_sessao_votacao_pauta".equals(violacao.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
