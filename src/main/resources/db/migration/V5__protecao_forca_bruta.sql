-- =====================================================================
-- Proteção contra força bruta: bloqueio temporário após falhas de senha.
--
-- Dois contadores independentes por usuário:
--   * login      -> tentativas na tela/endpoint de login;
--   * senha_op   -> senha de confirmação das operações (transferência, encerramento de conta).
-- Separados de propósito: errar no login não deve travar quem já está logado, e uma sessão roubada
-- testando a senha em /api/transferencia não consome o orçamento de tentativas do login.
--
-- Cada contador é uma janela: falhas_* conta as falhas desde inicio_falhas_*; ao atingir o limite,
-- *_bloqueado_ate recebe o fim do bloqueio. Tudo é atualizado com UPDATE atômico (sem lock explícito).
--
-- Fica no próprio usuário (e não só na auditoria) porque a auditoria é "melhor esforço": se a gravação
-- dela falhar, um controle de segurança baseado nela deixaria de contar tentativas.
-- =====================================================================

ALTER TABLE usuarios ADD COLUMN falhas_login          INTEGER      NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN inicio_falhas_login   TIMESTAMP(6);
ALTER TABLE usuarios ADD COLUMN login_bloqueado_ate   TIMESTAMP(6);

ALTER TABLE usuarios ADD COLUMN falhas_senha_op         INTEGER      NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN inicio_falhas_senha_op  TIMESTAMP(6);
ALTER TABLE usuarios ADD COLUMN senha_op_bloqueada_ate  TIMESTAMP(6);
