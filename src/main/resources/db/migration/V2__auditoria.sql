-- =====================================================================
-- Histórico de auditoria das operações.
-- Tabela append-only: UPDATE e DELETE são bloqueados por trigger,
-- então nem um bug da aplicação consegue reescrever o histórico.
-- =====================================================================

CREATE TABLE auditoria (
    id            UUID          NOT NULL,
    data_hora     TIMESTAMP(6)  NOT NULL,
    usuario_email VARCHAR(120)  NOT NULL,
    acao          VARCHAR(40)   NOT NULL,
    sucesso       BOOLEAN       NOT NULL,
    detalhes      VARCHAR(500),
    ip_origem     VARCHAR(45),
    CONSTRAINT pk_auditoria PRIMARY KEY (id)
);
CREATE INDEX idx_auditoria_usuario_data ON auditoria (usuario_email, data_hora DESC);
CREATE INDEX idx_auditoria_acao_data    ON auditoria (acao, data_hora DESC);

CREATE FUNCTION auditoria_imutavel() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'A tabela de auditoria é append-only (% não permitido)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_imutavel
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_imutavel();
