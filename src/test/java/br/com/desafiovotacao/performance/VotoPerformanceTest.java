package br.com.desafiovotacao.performance;

import br.com.desafiovotacao.repository.VotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class VotoPerformanceTest {
    private static final Logger log = LoggerFactory.getLogger(VotoPerformanceTest.class);

    @Test
    void deveContabilizarGrandeVolumeNoPostgresComRollback() throws Exception {
        int quantidade = Integer.parseInt(System.getProperty("performance.votes", "100000"));
        assertThat(quantidade).as("performance.votes deve ser positivo").isPositive();

        var ambiente = new StandardEnvironment();
        ambiente.getPropertySources().addLast(new PropertiesPropertySource("application",
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))));
        var datasource = new DriverManagerDataSource(
                ambiente.getRequiredProperty("spring.datasource.url"),
                ambiente.getRequiredProperty("spring.datasource.username"),
                ambiente.getRequiredProperty("spring.datasource.password"));
        var fabrica = new LocalContainerEntityManagerFactoryBean();
        fabrica.setDataSource(datasource);
        fabrica.setPackagesToScan("br.com.desafiovotacao.entity");
        fabrica.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        // Apenas valida o schema existente: não executa Flyway nem cria/altera tabelas.
        fabrica.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate",
                "hibernate.default_schema", "votacao"));
        fabrica.afterPropertiesSet();
        try (var em = fabrica.getObject().createEntityManager()) {
            var transacao = em.getTransaction();
            Long pautaId = null;
            transacao.begin();
            try {
                long inicio = System.nanoTime();
                pautaId = ((Number) em.createNativeQuery("""
                        insert into votacao.pauta(titulo, descricao, data_criacao)
                        values (:titulo, 'Massa temporária de performance', localtimestamp)
                        returning id
                        """).setParameter("titulo", "Performance " + UUID.randomUUID())
                        .getSingleResult()).longValue();
                em.createNativeQuery("""
                        insert into votacao.sessao_votacao(pauta_id, data_abertura, data_encerramento)
                        values (:pauta, localtimestamp, localtimestamp + interval '1 hour')
                        """).setParameter("pauta", pautaId).executeUpdate();
                int inseridos = em.createNativeQuery("""
                        insert into votacao.voto(pauta_id, associado_id, opcao, data_voto)
                        select :pauta, 'performance-' || n,
                               case when n % 2 = 0 then 'SIM' else 'NAO' end, localtimestamp
                        from generate_series(1, :quantidade) as serie(n)
                        """).setParameter("pauta", pautaId).setParameter("quantidade", quantidade)
                        .executeUpdate();
                long preparacao = System.nanoTime() - inicio;
                assertThat(inseridos).isEqualTo(quantidade);

                var repository = new JpaRepositoryFactory(em).getRepository(VotoRepository.class);
                inicio = System.nanoTime();
                var resultado = repository.contabilizarPorPautaId(pautaId);
                long contabilizacao = System.nanoTime() - inicio;
                assertThat(resultado.votosSim()).isEqualTo(quantidade / 2L);
                assertThat(resultado.votosNao()).isEqualTo(quantidade - quantidade / 2L);
                assertThat(resultado.totalVotos()).isEqualTo(quantidade);
                assertThat(resultado.votosSim() + resultado.votosNao()).isEqualTo(resultado.totalVotos());
                log.info("""

                        ========================================
                        TESTE DE PERFORMANCE - DESAFIO VOTACAO
                        Votos preparados: {}
                        SIM: {}
                        NAO: {}
                        TOTAL: {}
                        Tempo de preparacao: {} ms
                        Tempo de contabilizacao: {} ms
                        ========================================
                        """, inseridos, resultado.votosSim(), resultado.votosNao(), resultado.totalVotos(),
                        TimeUnit.NANOSECONDS.toMillis(preparacao),
                        TimeUnit.NANOSECONDS.toMillis(contabilizacao));
            } finally {
                // Nunca faz commit: inclusive falhas de asserção desfazem exclusivamente esta transação.
                if (transacao.isActive()) {
                    transacao.rollback();
                }
            }
            for (String consulta : new String[] {
                    "select count(*) from votacao.voto where pauta_id = :pauta",
                    "select count(*) from votacao.sessao_votacao where pauta_id = :pauta",
                    "select count(*) from votacao.pauta where id = :pauta"}) {
                long restantes = ((Number) em.createNativeQuery(consulta)
                        .setParameter("pauta", pautaId).getSingleResult()).longValue();
                assertThat(restantes).as("Registros do benchmark após rollback").isZero();
            }
            log.info("Rollback confirmado: pauta, sessão e votos do benchmark ausentes.");
        } finally {
            fabrica.destroy();
        }
    }
}
