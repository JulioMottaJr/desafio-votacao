package br.com.desafiovotacao.service.impl;

import br.com.desafiovotacao.client.AssociadoClient;
import br.com.desafiovotacao.dto.ContabilizacaoVotosResponse;
import br.com.desafiovotacao.entity.Pauta;
import br.com.desafiovotacao.entity.SessaoVotacao;
import br.com.desafiovotacao.exception.PautaNaoEncontradaException;
import br.com.desafiovotacao.repository.PautaRepository;
import br.com.desafiovotacao.repository.SessaoVotacaoRepository;
import br.com.desafiovotacao.repository.VotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContabilizacaoVotosServiceTest {

    @Mock
    private PautaRepository pautaRepository;
    @Mock
    private SessaoVotacaoRepository sessaoRepository;
    @Mock
    private VotoRepository votoRepository;

    @Mock
    private AssociadoClient associadoClient;

    private VotoServiceImpl service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void configurar() {
        service = new VotoServiceImpl(pautaRepository, sessaoRepository, votoRepository, clock, associadoClient);
    }

    @ParameterizedTest
    @CsvSource({
            "7, 3, 10",
            "5, 0, 5",
            "0, 8, 8",
            "0, 0, 0",
            "3000000000, 2000000000, 5000000000"
    })
    void deveRetornarContagensAgregadasSemCarregarVotos(long sim, long nao, long total) {
        when(pautaRepository.existsById(1L)).thenReturn(true);
        when(votoRepository.contabilizarPorPautaId(1L))
                .thenReturn(new ContabilizacaoVotosResponse(1L, sim, nao, total));

        ContabilizacaoVotosResponse resultado = service.contabilizar(1L);

        assertThat(resultado.pautaId()).isEqualTo(1L);
        assertThat(resultado.votosSim()).isEqualTo(sim);
        assertThat(resultado.votosNao()).isEqualTo(nao);
        assertThat(resultado.totalVotos()).isEqualTo(total);
        assertThat(resultado.totalVotos()).isEqualTo(resultado.votosSim() + resultado.votosNao());
        verify(pautaRepository).existsById(1L);
        verify(votoRepository).contabilizarPorPautaId(1L);
        // Qualquer busca de entidades ou consulta extra também faz este teste falhar.
        verifyNoMoreInteractions(pautaRepository, votoRepository);
        verifyNoInteractions(sessaoRepository);
    }

    @Test
    void deveRejeitarPautaInexistenteSemConsultarVotos() {
        when(pautaRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.contabilizar(1L))
                .isInstanceOf(PautaNaoEncontradaException.class);

        verifyNoInteractions(votoRepository, sessaoRepository);
    }

    @Test
    void deveContabilizarMesmoComSessaoEncerrada() {
        LocalDateTime agora = LocalDateTime.now(clock);
        Pauta pauta = new Pauta("Título", "Descrição", agora.minusDays(1));
        SessaoVotacao encerrada = new SessaoVotacao(pauta, agora.minusMinutes(5), agora.minusMinutes(1));
        // O cenário possui sessão encerrada, mas a contabilização não deve consultá-la.
        lenient().when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(encerrada));
        when(pautaRepository.existsById(1L)).thenReturn(true);
        ContabilizacaoVotosResponse esperado = new ContabilizacaoVotosResponse(1L, 4, 2, 6);
        when(votoRepository.contabilizarPorPautaId(1L)).thenReturn(esperado);

        assertThat(service.contabilizar(1L)).isEqualTo(esperado);
        verifyNoInteractions(sessaoRepository);
    }
}
