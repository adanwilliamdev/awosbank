package com.awosbank.api.service;

import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.util.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ContaService contaService;
    private final PasswordEncoder passwordEncoder;

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }

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

        return usuarioRepository.save(usuario);
    }
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
        return usuarioRepository.save(u);
    }

    /** E-mail é case-insensitive na prática: armazena e busca sempre em minúsculas. */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
