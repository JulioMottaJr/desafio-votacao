package br.com.desafiovotacao.service.impl;

import br.com.desafiovotacao.dto.RegistrarVotoRequest;
import br.com.desafiovotacao.dto.VotoResponse;
import br.com.desafiovotacao.entity.OpcaoVoto;
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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotoServiceImplTest {

    @Mock
    private PautaRepository pautaRepository;
    @Mock
    private SessaoVotacaoRepository sessaoRepository;
    @Mock
    private VotoRepository votoRepository;

    private VotoServiceImpl service;
    private Pauta pauta;
    private SessaoVotacao sessao;
    private Clock clock;
    private final LocalDateTime agora = LocalDateTime.of(2026, 9, 20, 10, 0);
    private final RegistrarVotoRequest request = new RegistrarVotoRequest("00123456", OpcaoVoto.SIM);

    @BeforeEach
    void configurar() {
        clock = spy(Clock.fixed(Instant.parse("2026-09-20T13:00:00Z"), ZoneId.of("America/Sao_Paulo")));
        service = new VotoServiceImpl(pautaRepository, sessaoRepository, votoRepository, clock);
        pauta = new Pauta("Título", "Descrição", agora.minusDays(1));
        ReflectionTestUtils.setField(pauta, "id", 1L);
        sessao = new SessaoVotacao(pauta, agora.minusMinutes(1), agora.plusMinutes(1));
    }

    private void prepararSessaoAberta() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
    }

    @ParameterizedTest
    @EnumSource(OpcaoVoto.class)
    void devePersistirVotoComHorarioDoClockSemAlterarSessao(OpcaoVoto opcao) {
        prepararSessaoAberta();
        when(votoRepository.saveAndFlush(any(Voto.class))).thenAnswer(invocation -> {
            Voto voto = invocation.getArgument(0);
            assertThat(voto.getId()).isNull();
            assertThat(voto.getPauta()).isSameAs(pauta);
            assertThat(voto.getAssociadoId()).isEqualTo("00123456");
            assertThat(voto.getOpcao()).isEqualTo(opcao);
            assertThat(voto.getDataVoto()).isEqualTo(agora);
            ReflectionTestUtils.setField(voto, "id", 3L);
            return voto;
        });

        VotoResponse response = service.registrar(1L, new RegistrarVotoRequest("00123456", opcao));

        assertThat(response).isEqualTo(new VotoResponse(3L, 1L, "00123456", opcao, agora));
        assertThat(sessao.getDataAbertura()).isEqualTo(agora.minusMinutes(1));
        assertThat(sessao.getDataEncerramento()).isEqualTo(agora.plusMinutes(1));
        verify(sessaoRepository).findByPautaId(1L);
        verifyNoMoreInteractions(sessaoRepository);
        verify(votoRepository).existsByPautaIdAndAssociadoId(1L, "00123456");
        verify(votoRepository).saveAndFlush(any(Voto.class));
        verify(clock, times(1)).instant();
    }

    @Test
    void deveRejeitarPautaInexistente() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(1L, request)).isInstanceOf(PautaNaoEncontradaException.class);

        verifyNoInteractions(sessaoRepository, votoRepository);
    }

    @Test
    void deveRejeitarPautaSemSessao() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(1L, request)).isInstanceOf(SessaoNaoEncontradaException.class);

        verifyNoInteractions(votoRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void deveRejeitarNoEncerramentoOuAposEle(int segundosAteEncerramento) {
        sessao = new SessaoVotacao(pauta, agora.minusMinutes(1), agora.plusSeconds(segundosAteEncerramento));
        prepararSessaoAberta();

        assertThatThrownBy(() -> service.registrar(1L, request)).isInstanceOf(SessaoEncerradaException.class);

        verifyNoInteractions(votoRepository);
    }

    @Test
    void deveRejeitarAssociadoQueJaVotou() {
        prepararSessaoAberta();
        when(votoRepository.existsByPautaIdAndAssociadoId(1L, request.associadoId())).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(1L, request)).isInstanceOf(VotoDuplicadoException.class);

        verify(votoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveTraduzirViolacaoConcorrenteDaConstraintDeVotoDuplicado() {
        prepararSessaoAberta();
        ConstraintViolationException causa = new ConstraintViolationException(
                "Duplicidade", new SQLException("Duplicidade", "23505"), "uk_voto_pauta_associado");
        when(votoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Falha", causa));

        assertThatThrownBy(() -> service.registrar(1L, request)).isInstanceOf(VotoDuplicadoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fk_voto_pauta", "ck_voto_opcao"})
    void naoDeveMascararOutrasConstraints(String constraint) {
        prepararSessaoAberta();
        ConstraintViolationException causa = new ConstraintViolationException(
                "Integridade", new SQLException("Integridade"), constraint);
        DataIntegrityViolationException falha = new DataIntegrityViolationException("Falha", causa);
        when(votoRepository.saveAndFlush(any())).thenThrow(falha);

        assertThatThrownBy(() -> service.registrar(1L, request)).isSameAs(falha);
    }
}
