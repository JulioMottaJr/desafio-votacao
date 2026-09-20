CREATE TABLE votacao.sessao_votacao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pauta_id BIGINT NOT NULL,
    data_abertura TIMESTAMP NOT NULL,
    data_encerramento TIMESTAMP NOT NULL,
    CONSTRAINT fk_sessao_votacao_pauta FOREIGN KEY (pauta_id) REFERENCES votacao.pauta(id),
    CONSTRAINT uk_sessao_votacao_pauta UNIQUE (pauta_id)
);
