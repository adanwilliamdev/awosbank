-- =====================================================================
-- Auditoria: vincula cada registro ao ID estável do usuário.
--
-- Antes, o histórico era consultado por e-mail. Como o encerramento de conta apaga o usuário e
-- libera o e-mail, quem se cadastrasse depois com o mesmo e-mail enxergaria o histórico do
-- titular anterior. Agora a consulta usa usuario_id (UUID, nunca reaproveitado).
--
-- Sem FOREIGN KEY de propósito: o usuário é excluído no encerramento da conta, e o registro de
-- auditoria precisa sobreviver a isso. usuario_id é NULL quando o ator não é um usuário
-- cadastrado (ex.: tentativa de login com e-mail inexistente).
-- =====================================================================

ALTER TABLE auditoria ADD COLUMN usuario_id UUID;

-- Backfill dos registros já existentes. A tabela é append-only (trigger da V2), então o trigger é
-- desligado SOMENTE durante este UPDATE; o Flyway executa a migration numa transação, e um erro
-- aqui desfaz tudo (inclusive o DISABLE).
--
-- O registro só é atribuído ao usuário atual do e-mail se aconteceu DEPOIS da abertura da conta
-- dele (contas.data_abertura). Registros anteriores pertencem a um titular antigo daquele e-mail
-- (conta já encerrada) ou a tentativas feitas antes do cadastro, e ficam sem dono (NULL), portanto
-- invisíveis para o usuário atual. Registros de usuários já excluídos também permanecem NULL.
ALTER TABLE auditoria DISABLE TRIGGER trg_auditoria_imutavel;

UPDATE auditoria a
   SET usuario_id = u.id
  FROM usuarios u
  JOIN contas c ON c.id = u.conta_id
 WHERE u.email = a.usuario_email
   AND a.data_hora >= c.data_abertura;

ALTER TABLE auditoria ENABLE TRIGGER trg_auditoria_imutavel;

CREATE INDEX idx_auditoria_usuario_id_data ON auditoria (usuario_id, data_hora DESC);
