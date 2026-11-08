-- =====================================================================
-- Idempotência nas transferências.
--
-- Um duplo clique ou um retry de rede reenvia a MESMA requisição. O cliente passa a mandar uma
-- chave (header Idempotency-Key na API, token oculto no formulário web) e o servidor a grava aqui:
-- repetir a chave devolve a transferência já feita em vez de criar outra.
--
-- A unicidade é por (conta de origem, chave): clientes diferentes podem usar o mesmo valor sem
-- colidir, e uma chave de um cliente nunca revela transação de outro.
--
-- Coluna anulável: depósitos, saques e transferências antigas não têm chave. Em PostgreSQL (e H2)
-- vários NULL não violam a UNIQUE, então só as transferências com chave são protegidas.
-- O lock pessimista da conta de origem serializa as tentativas concorrentes; esta constraint é a
-- última barreira caso algum caminho de código esqueça de consultar a chave antes de gravar.
-- =====================================================================

ALTER TABLE transacoes ADD COLUMN chave_idempotencia VARCHAR(64);

ALTER TABLE transacoes
    ADD CONSTRAINT uk_transacoes_origem_chave UNIQUE (conta_origem_id, chave_idempotencia);
