package com.awosbank.api.service;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.domain.repository.AuditoriaRepository;
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

@Slf4j
@Service
public class AuditoriaService {

    private static final int MAX_EMAIL = 120;
    private static final int MAX_DETALHES = 500;

    private final AuditoriaRepository repository;
    private final TransactionTemplate transacaoIndependente;

    public AuditoriaService(AuditoriaRepository repository, PlatformTransactionManager txManager) {
        this.repository = repository;
        this.transacaoIndependente = new TransactionTemplate(txManager);
        // O registro de auditoria é gravado numa transação PRÓPRIA: sobrevive ao rollback da operação auditada.
        this.transacaoIndependente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Grava um registro de auditoria. Nunca lança exceção: uma falha ao auditar não pode derrubar
     * (nem mascarar o resultado de) a operação do cliente — o problema vai para o log da aplicação.
     */
    public void registrar(AcaoAuditoria acao, String usuarioEmail, boolean sucesso, String detalhes) {
        try {
            Auditoria registro = Auditoria.builder()
                    .dataHora(LocalDateTime.now())
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

    /** Histórico do próprio usuário, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public Page<Auditoria> listarDoUsuario(String usuarioEmail, Pageable pageable) {
        return repository.findByUsuarioEmailOrderByDataHoraDesc(normalizarEmail(usuarioEmail), pageable);
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
