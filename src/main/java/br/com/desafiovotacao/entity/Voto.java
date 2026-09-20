package br.com.desafiovotacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "voto", schema = "votacao", uniqueConstraints =
        @UniqueConstraint(name = "uk_voto_pauta_associado", columnNames = {"pauta_id", "associado_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_voto_pauta"))
    private Pauta pauta;

    @Column(name = "associado_id", nullable = false, length = 100)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "opcao", nullable = false, length = 3)
    private OpcaoVoto opcao;

    @Column(name = "data_voto", nullable = false)
    private LocalDateTime dataVoto;

    public Voto(Pauta pauta, String associadoId, OpcaoVoto opcao, LocalDateTime dataVoto) {
        this.pauta = pauta;
        this.associadoId = associadoId;
        this.opcao = opcao;
        this.dataVoto = dataVoto;
    }
}
