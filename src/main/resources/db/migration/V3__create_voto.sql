CREATE TABLE votacao.voto (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pauta_id BIGINT NOT NULL,
    associado_id VARCHAR(100) NOT NULL,
    opcao VARCHAR(3) NOT NULL,
    data_voto TIMESTAMP NOT NULL,
    CONSTRAINT fk_voto_pauta FOREIGN KEY (pauta_id) REFERENCES votacao.pauta(id),
    CONSTRAINT uk_voto_pauta_associado UNIQUE (pauta_id, associado_id),
    CONSTRAINT ck_voto_opcao CHECK (opcao IN ('SIM', 'NAO'))
);
