package com.awosbank.api.service;

import com.awosbank.api.audit.Auditado;
import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.util.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** SQLSTATE padrão (ANSI) de violação de UNIQUE: o mesmo no PostgreSQL e no H2. */
    private static final String SQLSTATE_VIOLACAO_UNICIDADE = "23505";

    private final UsuarioRepository usuarioRepository;
    private final ContaService contaService;
    private final PasswordEncoder passwordEncoder;

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }

    @Auditado(AcaoAuditoria.CADASTRO)
    @Transactional
    public Usuario cadastrar(CadastroDTO dto) {
        if (!dto.getSenha().equals(dto.getConfirmarSenha())) {
            throw new BusinessException("Senhas não conferem");
        }
        if (!DocumentoValidator.isValid(dto.getCpfCnpj())) {
            throw new BusinessException("CPF/CNPJ inválido");
        }
        String documentoLimpo = dto.getCpfCnpj().replaceAll("\\D", "");
        String email = normalizarEmail(dto.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("Email já cadastrado");
        }
        if (usuarioRepository.existsByCpfCnpj(documentoLimpo)) {
            throw new BusinessException("CPF/CNPJ já cadastrado");
        }

        Conta conta = contaService.criarConta(dto.getTipoConta());
        conta.setStatus(StatusConta.ATIVA);

        Usuario usuario = Usuario.builder()
                .nomeCompleto(dto.getNomeCompleto())
                .cpfCnpj(documentoLimpo)
                .email(email)
                .senha(passwordEncoder.encode(dto.getSenha()))
                .roles(Set.of("ROLE_CLIENTE"))
                .conta(conta)
                .build();

        try {
            // saveAndFlush (e não save): o INSERT precisa acontecer AQUI, dentro do try. Com save() ele só
            // iria ao banco no commit, depois do método, e a violação escaparia deste catch.
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Corrida: outra requisição cadastrou o mesmo e-mail/CPF entre o existsBy... e o INSERT.
            // A constraint UNIQUE do banco decide quem ganhou; quem perdeu recebe o mesmo erro de negócio
            // da checagem prévia. (Não dá para reconsultar o banco aqui: no PostgreSQL a transação já está abortada.)
            String causa = causaDeViolacaoDeUnicidade(e);
            if (causa == null) {
                throw e; // outra violação (NOT NULL, tamanho…): bug de verdade, deve ir para o log com código
            }
            if (causa.contains("email")) {
                throw new BusinessException("Email já cadastrado");
            }
            if (causa.contains("cpf")) {
                throw new BusinessException("CPF/CNPJ já cadastrado");
            }
            log.warn("Cadastro recusado por violação de unicidade fora de email/cpf_cnpj "
                    + "(provável colisão no número da conta); o cliente pode tentar de novo");
            throw new BusinessException("Não foi possível concluir o cadastro agora. Tente novamente.");
        }
    }

    @Auditado(AcaoAuditoria.ATUALIZACAO_PERFIL)
    @Transactional
    public Usuario atualizarPerfil(String emailAtual, com.awosbank.api.dto.PerfilDTO dto) {
        Usuario u = buscarPorEmail(emailAtual);
        String novoEmail = normalizarEmail(dto.getEmail());
        if (!u.getEmail().equals(novoEmail)
                && usuarioRepository.existsByEmail(novoEmail)) {
            throw new BusinessException("Novo email já cadastrado");
        }
        u.setNomeCompleto(dto.getNomeCompleto());
        u.setEmail(novoEmail);
        u.setTelefone(dto.getTelefone());
        u.setDataNascimento(dto.getDataNascimento());
        u.setEndereco(dto.getEndereco());
        try {
            // saveAndFlush: o UPDATE (e a checagem da UNIQUE) precisa acontecer dentro do try, não no commit.
            return usuarioRepository.saveAndFlush(u);
        } catch (DataIntegrityViolationException e) {
            // Corrida: outro usuário assumiu o e-mail entre o existsByEmail e o UPDATE.
            // O e-mail é a única coluna única que o perfil altera.
            if (causaDeViolacaoDeUnicidade(e) == null) {
                throw e;
            }
            throw new BusinessException("Novo email já cadastrado");
        }
    }

    /**
     * Se a exceção é uma violação de UNIQUE (SQLSTATE 23505), devolve a mensagem do banco em minúsculas
     * (traz a constraint e/ou a coluna: {@code uk_usuarios_email}, {@code Key (cpf_cnpj)=...}, {@code USUARIOS(EMAIL…)});
     * caso contrário, {@code null}. Olha o SQLSTATE, e não o texto, para não confundir com outras violações
     * (ex.: NOT NULL na coluna email).
     */
    private static String causaDeViolacaoDeUnicidade(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && SQLSTATE_VIOLACAO_UNICIDADE.equals(sql.getSQLState())) {
                return String.valueOf(sql.getMessage()).toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    /** E-mail é case-insensitive na prática: armazena e busca sempre em minúsculas. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
