package br.com.desafiovotacao.service.impl;

import br.com.desafiovotacao.dto.CriarPautaRequest;
import br.com.desafiovotacao.dto.PautaResponse;
import br.com.desafiovotacao.entity.Pauta;
import br.com.desafiovotacao.repository.PautaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PautaServiceImplTest {

    @Mock
    private PautaRepository repository;

    @InjectMocks
    private PautaServiceImpl service;

    @Test
    void deveCriarPautaComDataDoBackendERetornarDadosPersistidos() {
        CriarPautaRequest request = new CriarPautaRequest("Aquisição de veículos", "Compra de dois veículos.");
        LocalDateTime inicio = LocalDateTime.now();
        when(repository.save(any(Pauta.class))).thenAnswer(invocation -> {
            Pauta pauta = invocation.getArgument(0);
            assertThat(pauta.getId()).isNull();
            assertThat(pauta.getTitulo()).isEqualTo(request.titulo());
            assertThat(pauta.getDescricao()).isEqualTo(request.descricao());
            assertThat(pauta.getDataCriacao()).isBetween(inicio, LocalDateTime.now());
            ReflectionTestUtils.setField(pauta, "id", 1L);
            return pauta;
        });

        PautaResponse response = service.criar(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.titulo()).isEqualTo(request.titulo());
        assertThat(response.descricao()).isEqualTo(request.descricao());
        assertThat(response.dataCriacao()).isBetween(inicio, LocalDateTime.now());
        verify(repository).save(any(Pauta.class));
    }
}
