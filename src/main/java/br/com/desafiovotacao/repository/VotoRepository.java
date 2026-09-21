package br.com.desafiovotacao.repository;

import br.com.desafiovotacao.dto.ContabilizacaoVotosResponse;
import br.com.desafiovotacao.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaIdAndAssociadoId(Long pautaId, String associadoId);

    @Query("""
            select new br.com.desafiovotacao.dto.ContabilizacaoVotosResponse(
                :pautaId,
                count(case when v.opcao = br.com.desafiovotacao.entity.OpcaoVoto.SIM then 1 else null end),
                count(case when v.opcao = br.com.desafiovotacao.entity.OpcaoVoto.NAO then 1 else null end),
                count(v))
            from Voto v
            where v.pauta.id = :pautaId
            """)
    ContabilizacaoVotosResponse contabilizarPorPautaId(@Param("pautaId") Long pautaId);
}
