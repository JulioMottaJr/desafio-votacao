package br.com.desafiovotacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessao_votacao", schema = "votacao", uniqueConstraints =
        @UniqueConstraint(name = "uk_sessao_votacao_pauta", columnNames = "pauta_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessaoVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sessao_votacao_pauta"))
    private Pauta pauta;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_encerramento", nullable = false)
    private LocalDateTime dataEncerramento;

    public SessaoVotacao(Pauta pauta, LocalDateTime dataAbertura, LocalDateTime dataEncerramento) {
        this.pauta = pauta;
        this.dataAbertura = dataAbertura;
        this.dataEncerramento = dataEncerramento;
    }
}
