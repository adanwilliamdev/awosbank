-- =====================================================================
-- AWOS Bank — schema inicial (PostgreSQL)
-- Espelha as entidades JPA. O Hibernate roda com ddl-auto=validate:
-- se uma entidade divergir deste script, a aplicação não sobe.
-- =====================================================================

CREATE TABLE contas (
    id            UUID           NOT NULL,
    numero_conta  VARCHAR(20)    NOT NULL,
    agencia       VARCHAR(10)    NOT NULL,
    saldo         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    tipo_conta    VARCHAR(20)    NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    data_abertura TIMESTAMP(6)   NOT NULL,
    CONSTRAINT pk_contas PRIMARY KEY (id),
    CONSTRAINT uk_contas_numero UNIQUE (numero_conta),
    -- última barreira contra saldo negativo, mesmo que a regra de negócio falhe
    CONSTRAINT ck_contas_saldo_nao_negativo CHECK (saldo >= 0)
);

CREATE TABLE usuarios (
    id              UUID         NOT NULL,
    nome_completo   VARCHAR(120) NOT NULL,
    cpf_cnpj        VARCHAR(20)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    senha           VARCHAR(255) NOT NULL,
    telefone        VARCHAR(20),
    data_nascimento VARCHAR(20),
    endereco        VARCHAR(200),
    conta_id        UUID,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_cpf_cnpj UNIQUE (cpf_cnpj),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_conta UNIQUE (conta_id),
    CONSTRAINT fk_usuarios_conta FOREIGN KEY (conta_id) REFERENCES contas (id)
);

CREATE TABLE usuario_roles (
    usuario_id UUID         NOT NULL,
    role       VARCHAR(255),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_usuario_roles_usuario ON usuario_roles (usuario_id);

CREATE TABLE pix_keys (
    id           UUID          NOT NULL,
    usuario_id   UUID          NOT NULL,
    tipo         VARCHAR(20)   NOT NULL,
    chave        VARCHAR(120)  NOT NULL,
    data_criacao TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_pix_keys PRIMARY KEY (id),
    CONSTRAINT uk_pix_keys_chave UNIQUE (chave),
    CONSTRAINT fk_pix_keys_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
CREATE INDEX idx_pix_keys_usuario ON pix_keys (usuario_id);

CREATE TABLE transacoes (
    id                 UUID           NOT NULL,
    conta_origem_id    UUID,
    conta_destino_id   UUID,
    valor              NUMERIC(19, 2) NOT NULL,
    tipo               VARCHAR(20)    NOT NULL,
    data_hora          TIMESTAMP(6)   NOT NULL,
    comprovante_codigo VARCHAR(40)    NOT NULL,
    CONSTRAINT pk_transacoes PRIMARY KEY (id),
    CONSTRAINT uk_transacoes_comprovante UNIQUE (comprovante_codigo),
    CONSTRAINT fk_transacoes_origem FOREIGN KEY (conta_origem_id) REFERENCES contas (id),
    CONSTRAINT fk_transacoes_destino FOREIGN KEY (conta_destino_id) REFERENCES contas (id),
    CONSTRAINT ck_transacoes_valor_positivo CHECK (valor > 0)
);
-- índices das consultas de extrato (origem OU destino, ordenado por data)
CREATE INDEX idx_transacoes_origem_data  ON transacoes (conta_origem_id, data_hora DESC);
CREATE INDEX idx_transacoes_destino_data ON transacoes (conta_destino_id, data_hora DESC);
