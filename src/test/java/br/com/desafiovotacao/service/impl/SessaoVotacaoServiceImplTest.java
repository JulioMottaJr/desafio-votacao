package br.com.desafiovotacao.service.impl;

import br.com.desafiovotacao.dto.AbrirSessaoRequest;
import br.com.desafiovotacao.dto.SessaoVotacaoResponse;
import br.com.desafiovotacao.entity.Pauta;
import br.com.desafiovotacao.entity.SessaoVotacao;
import br.com.desafiovotacao.exception.PautaNaoEncontradaException;
import br.com.desafiovotacao.exception.SessaoJaExistenteException;
import br.com.desafiovotacao.repository.PautaRepository;
import br.com.desafiovotacao.repository.SessaoVotacaoRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessaoVotacaoServiceImplTest {

    @Mock
    private PautaRepository pautaRepository;
    @Mock
    private SessaoVotacaoRepository sessaoRepository;

    private SessaoVotacaoServiceImpl service;
    private Pauta pauta;
    private final LocalDateTime abertura = LocalDateTime.of(2026, 9, 19, 17, 0);

    @BeforeEach
    void configurar() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-19T20:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new SessaoVotacaoServiceImpl(pautaRepository, sessaoRepository, clock);
        pauta = new Pauta("Título", "Descrição", abertura.minusDays(1));
        ReflectionTestUtils.setField(pauta, "id", 1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {1, 5, Integer.MAX_VALUE})
    void deveAbrirSessaoComDuracaoInformadaOuUmMinutoPorPadrao(Integer duracao) {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        LocalDateTime encerramento = abertura.plusMinutes(duracao == null ? 1 : duracao);
        when(sessaoRepository.saveAndFlush(any(SessaoVotacao.class))).thenAnswer(invocation -> {
            SessaoVotacao sessao = invocation.getArgument(0);
            assertThat(sessao.getId()).isNull();
            assertThat(sessao.getPauta()).isSameAs(pauta);
            assertThat(sessao.getDataAbertura()).isEqualTo(abertura);
            assertThat(sessao.getDataEncerramento()).isEqualTo(encerramento);
            ReflectionTestUtils.setField(sessao, "id", 2L);
            return sessao;
        });

        SessaoVotacaoResponse response = service.abrir(1L, new AbrirSessaoRequest(duracao));

        assertThat(response).isEqualTo(new SessaoVotacaoResponse(2L, 1L, abertura, encerramento));
        verify(sessaoRepository).existsByPautaId(1L);
        verify(sessaoRepository).saveAndFlush(any(SessaoVotacao.class));
    }

    @Test
    void deveRejeitarPautaInexistente() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrir(1L, new AbrirSessaoRequest(5)))
                .isInstanceOf(PautaNaoEncontradaException.class);

        verifyNoInteractions(sessaoRepository);
    }

    @Test
    void deveRejeitarPautaQueJaPossuiSessao() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.existsByPautaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.abrir(1L, new AbrirSessaoRequest(5)))
                .isInstanceOf(SessaoJaExistenteException.class);

        verify(sessaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveTraduzirViolacaoDaConstraintDeUnicidadeEmConflito() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        ConstraintViolationException causa = new ConstraintViolationException(
                "Duplicidade", new SQLException("Duplicidade", "23505"), "uk_sessao_votacao_pauta");
        when(sessaoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Falha", causa));

        assertThatThrownBy(() -> service.abrir(1L, new AbrirSessaoRequest(5)))
                .isInstanceOf(SessaoJaExistenteException.class);
    }

    @Test
    void naoDeveMascararOutrasFalhasDeIntegridadeComoSessaoDuplicada() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        ConstraintViolationException causa = new ConstraintViolationException(
                "Referência inválida", new SQLException("Referência inválida", "23503"), "fk_sessao_votacao_pauta");
        DataIntegrityViolationException falha = new DataIntegrityViolationException("Falha", causa);
        when(sessaoRepository.saveAndFlush(any())).thenThrow(falha);

        assertThatThrownBy(() -> service.abrir(1L, new AbrirSessaoRequest(5))).isSameAs(falha);
    }
}
