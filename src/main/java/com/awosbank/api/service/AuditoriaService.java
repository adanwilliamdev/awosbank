package com.awosbank.api.service;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.domain.repository.AuditoriaRepository;
import com.awosbank.api.domain.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AuditoriaService {

    private static final int MAX_EMAIL = 120;
    private static final int MAX_DETALHES = 500;

    private final AuditoriaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final TransactionTemplate transacaoIndependente;

    public AuditoriaService(AuditoriaRepository repository, UsuarioRepository usuarioRepository,
                            PlatformTransactionManager txManager) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.transacaoIndependente = new TransactionTemplate(txManager);
        // O registro de auditoria é gravado numa transação PRÓPRIA: sobrevive ao rollback da operação auditada.
        this.transacaoIndependente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Grava um registro de auditoria. Nunca lança exceção: uma falha ao auditar não pode derrubar
     * (nem mascarar o resultado de) a operação do cliente — o problema vai para o log da aplicação.
     *
     * @param usuarioId    ID estável do dono do registro; {@code null} se o ator não é um usuário cadastrado
     *                     (o histórico do usuário é consultado por este ID, nunca pelo e-mail)
     * @param usuarioEmail e-mail do ator no momento do evento (informativo)
     */
    public void registrar(AcaoAuditoria acao, UUID usuarioId, String usuarioEmail, boolean sucesso, String detalhes) {
        try {
            Auditoria registro = Auditoria.builder()
                    .dataHora(LocalDateTime.now())
                    .usuarioId(usuarioId)
                    .usuarioEmail(normalizarEmail(usuarioEmail))
                    .acao(acao)
                    .sucesso(sucesso)
                    .detalhes(truncar(detalhes, MAX_DETALHES))
                    .ipOrigem(ipDaRequisicao())
                    .build();
            transacaoIndependente.executeWithoutResult(status -> repository.save(registro));
        } catch (RuntimeException e) {
            log.error("Falha ao gravar auditoria (acao={}, usuario={}): {}", acao, usuarioEmail, e.toString());
        }
    }

    /** Como {@link #registrar}, resolvendo o ID do usuário pelo e-mail (para quem só conhece o e-mail, ex.: eventos de login). */
    public void registrarPorEmail(AcaoAuditoria acao, String usuarioEmail, boolean sucesso, String detalhes) {
        registrar(acao, buscarIdPorEmail(usuarioEmail), usuarioEmail, sucesso, detalhes);
    }

    /**
     * ID do usuário atualmente dono do e-mail, ou {@code null} se não houver. Nunca lança exceção.
     *
     * <p>Deve ser chamado ANTES de operações que apagam o usuário (encerramento de conta): depois delas
     * o e-mail já não resolve para ninguém.
     */
    public UUID buscarIdPorEmail(String email) {
        if (email == null || email.isBlank()) return null;
        try {
            return usuarioRepository.findIdByEmail(UsuarioService.normalizarEmail(email)).orElse(null);
        } catch (RuntimeException e) {
            log.warn("Não foi possível resolver o ID do usuário para auditoria: {}", e.toString());
            return null;
        }
    }

    /**
     * Histórico do próprio usuário, do mais recente para o mais antigo. Identificado pelo ID, e não pelo
     * e-mail, para que ninguém herde o histórico de uma conta encerrada que usava o mesmo e-mail.
     */
    @Transactional(readOnly = true)
    public Page<Auditoria> listarDoUsuario(UUID usuarioId, Pageable pageable) {
        // Com ID nulo o Spring Data geraria "usuario_id IS NULL" e listaria registros de ninguém/de todos.
        if (usuarioId == null) return Page.empty(pageable);
        return repository.findByUsuarioIdOrderByDataHoraDesc(usuarioId, pageable);
    }

    private static String normalizarEmail(String email) {
        String e = (email == null || email.isBlank()) ? "anonimo" : UsuarioService.normalizarEmail(email);
        return truncar(e, MAX_EMAIL);
    }

    private static String truncar(String texto, int max) {
        if (texto == null) return null;
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    private static String ipDaRequisicao() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            return truncar(req.getRemoteAddr(), 45);
        }
        return null;
    }
}
