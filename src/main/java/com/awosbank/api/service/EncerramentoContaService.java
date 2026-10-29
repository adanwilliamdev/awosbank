package com.awosbank.api.service;

import com.awosbank.api.audit.Auditado;
import com.awosbank.api.config.FormatadorUtil;
import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.domain.repository.PixKeyRepository;
import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encerramento (exclusão) da conta bancária pelo próprio cliente.
 *
 * <p>A conta <b>não é apagada fisicamente</b>: as transações dela continuam existindo no extrato de
 * quem transferiu de/para ela (e por exigência de rastreabilidade). Ela passa a {@code ENCERRADA} —
 * o que já bloqueia depósitos, saques e transferências, pois todos exigem conta {@code ATIVA} — e os
 * dados do titular (usuário, login e chaves PIX) são removidos, liberando e-mail e CPF/CNPJ
 * para um novo cadastro.
 */
@Service
@RequiredArgsConstructor
public class EncerramentoContaService {

    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;
    private final PixKeyRepository pixKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final FormatadorUtil formatador;

    @PersistenceContext
    private EntityManager em;

    @Auditado(AcaoAuditoria.CONTA_ENCERRADA)
    @Transactional
    public void encerrar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(UsuarioService.normalizarEmail(email))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (senha == null || !passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new BusinessException("Senha incorreta");
        }
        if (usuario.getConta() == null) {
            throw new BusinessException("Usuário não possui conta");
        }

        // Lock + refresh: o saldo é conferido sobre o dado mais recente do banco, sem corrida
        // com uma transferência/depósito simultâneo (mesmo padrão do TransacaoService).
        Conta conta = contaRepository.findByIdForUpdate(usuario.getConta().getId())
                .orElseThrow(() -> new BusinessException("Conta não encontrada"));
        em.refresh(conta);

        if (conta.getStatus() == StatusConta.ENCERRADA) {
            throw new BusinessException("Conta já encerrada");
        }
        if (conta.getSaldo().signum() != 0) {
            throw new BusinessException("Não é possível encerrar a conta com saldo de "
                    + formatador.moeda(conta.getSaldo()) + ". Saque ou transfira o valor antes.");
        }

        conta.setStatus(StatusConta.ENCERRADA);
        contaRepository.save(conta);

        pixKeyRepository.deleteAll(pixKeyRepository.findByUsuario(usuario));
        pixKeyRepository.flush();

        // Solta a conta do usuário antes de excluí-lo: o cascade ALL de Usuario->Conta
        // tentaria remover a conta, que precisa ficar (transações referenciam ela).
        usuario.setConta(null);
        usuarioRepository.delete(usuario);
    }
}
