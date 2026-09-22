package br.com.desafiovotacao.service.impl;

import br.com.desafiovotacao.client.AssociadoClient;
import br.com.desafiovotacao.client.StatusElegibilidade;
import br.com.desafiovotacao.exception.AssociadoNaoHabilitadoException;

import br.com.desafiovotacao.dto.ContabilizacaoVotosResponse;
import br.com.desafiovotacao.dto.RegistrarVotoRequest;
import br.com.desafiovotacao.dto.VotoResponse;
import br.com.desafiovotacao.entity.Pauta;
import br.com.desafiovotacao.entity.SessaoVotacao;
import br.com.desafiovotacao.entity.Voto;
import br.com.desafiovotacao.exception.PautaNaoEncontradaException;
import br.com.desafiovotacao.exception.SessaoNaoEncontradaException;
import br.com.desafiovotacao.exception.SessaoEncerradaException;
import br.com.desafiovotacao.exception.VotoDuplicadoException;
import br.com.desafiovotacao.repository.PautaRepository;
import br.com.desafiovotacao.repository.SessaoVotacaoRepository;
import br.com.desafiovotacao.repository.VotoRepository;
import br.com.desafiovotacao.service.VotoService;
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
public class VotoServiceImpl implements VotoService {

    private final PautaRepository pautaRepository;
    private final SessaoVotacaoRepository sessaoVotacaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;
    private final AssociadoClient associadoClient;

    @Override
    @Transactional
    public VotoResponse registrar(Long pautaId, RegistrarVotoRequest request) {
        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        SessaoVotacao sessao = sessaoVotacaoRepository.findByPautaId(pautaId)
                .orElseThrow(() -> {
                    log.warn("Tentativa de voto em pauta sem sessão. pautaId={}", pautaId);
                    return new SessaoNaoEncontradaException(pautaId);
                });

        LocalDateTime agora = LocalDateTime.now(clock);
        if (!agora.isBefore(sessao.getDataEncerramento())) {
            log.warn("Tentativa de voto em sessão encerrada. pautaId={}", pautaId);
            throw new SessaoEncerradaException(pautaId);
        }
        if (votoRepository.existsByPautaIdAndAssociadoId(pautaId, request.associadoId())) {
            throw votoDuplicado(pautaId);
        }

        // No bônus, o identificador existente é utilizado como CPF na consulta externa.
        if (associadoClient.consultarElegibilidade(request.associadoId()).status()
                == StatusElegibilidade.UNABLE_TO_VOTE) {
            log.warn("Associado não habilitado para votar. pautaId={}", pautaId);
            throw new AssociadoNaoHabilitadoException();
        }

        Voto voto = new Voto(pauta, request.associadoId(), request.opcao(), agora);
        Voto salvo;
        try {
            // Identifica a violação de unicidade ainda dentro da operação transacional.
            salvo = votoRepository.saveAndFlush(voto);
        } catch (DataIntegrityViolationException exception) {
            if (violouUnicidadeDoVoto(exception)) {
                throw votoDuplicado(pautaId);
            }
            throw exception;
        }

        log.info("Voto registrado. pautaId={}, votoId={}", pautaId, salvo.getId());
        return new VotoResponse(salvo.getId(), salvo.getPauta().getId(), salvo.getAssociadoId(),
                salvo.getOpcao(), salvo.getDataVoto());
    }

    @Override
    @Transactional(readOnly = true)
    public ContabilizacaoVotosResponse contabilizar(Long pautaId) {
        log.debug("Iniciando contabilização de votos. pautaId={}", pautaId);
        if (!pautaRepository.existsById(pautaId)) {
            log.warn("Tentativa de contabilização de pauta inexistente. pautaId={}", pautaId);
            throw new PautaNaoEncontradaException(pautaId);
        }

        ContabilizacaoVotosResponse resultado = votoRepository.contabilizarPorPautaId(pautaId);
        log.debug("Votos contabilizados. pautaId={}, votosSim={}, votosNao={}, totalVotos={}",
                pautaId, resultado.votosSim(), resultado.votosNao(), resultado.totalVotos());
        return resultado;
    }

    private VotoDuplicadoException votoDuplicado(Long pautaId) {
        log.warn("Tentativa de voto duplicado. pautaId={}", pautaId);
        return new VotoDuplicadoException(pautaId);
    }

    private boolean violouUnicidadeDoVoto(Throwable exception) {
        for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && "uk_voto_pauta_associado".equals(violacao.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
