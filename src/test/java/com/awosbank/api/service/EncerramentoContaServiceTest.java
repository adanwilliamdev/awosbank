package com.awosbank.api.service;

import com.awosbank.api.config.FormatadorUtil;
import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.PixKey;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.domain.repository.PixKeyRepository;
import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncerramentoContaServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock ContaRepository contaRepository;
    @Mock PixKeyRepository pixKeyRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EntityManager em;

    EncerramentoContaService service;
    Conta conta;
    Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new EncerramentoContaService(usuarioRepository, contaRepository, pixKeyRepository,
                passwordEncoder, new FormatadorUtil());
        ReflectionTestUtils.setField(service, "em", em);

        conta = Conta.builder().id(UUID.randomUUID()).numeroConta("123456-7").agencia("0001")
                .saldo(BigDecimal.ZERO).tipoConta(TipoConta.CORRENTE).status(StatusConta.ATIVA).build();
        usuario = Usuario.builder().id(UUID.randomUUID()).email("maria@exemplo.com").senha("hash").conta(conta).build();

        when(usuarioRepository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(usuario));
        when(contaRepository.findByIdForUpdate(conta.getId())).thenReturn(Optional.of(conta));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
    }

    @Test
    void encerraContaComSaldoZeroRemoveUsuarioEChavesMasPreservaConta() {
        List<PixKey> chaves = List.of(PixKey.builder().usuario(usuario).chave("maria@exemplo.com").build());
        when(pixKeyRepository.findByUsuario(usuario)).thenReturn(chaves);

        service.encerrar("Maria@Exemplo.com", "123456");

        assertEquals(StatusConta.ENCERRADA, conta.getStatus());
        verify(contaRepository).save(conta);
        verify(pixKeyRepository).deleteAll(chaves);
        assertNull(usuario.getConta(), "a conta precisa ser solta do usuário para o cascade não removê-la");
        verify(usuarioRepository).delete(usuario);
        verify(contaRepository, never()).delete(any(Conta.class));
    }

    @Test
    void senhaIncorretaNaoEncerraNada() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.encerrar("maria@exemplo.com", "errada"));

        assertEquals("Senha incorreta", e.getMessage());
        assertEquals(StatusConta.ATIVA, conta.getStatus());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void recusaEncerrarContaComSaldo() {
        conta.setSaldo(new BigDecimal("10.50"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.encerrar("maria@exemplo.com", "123456"));

        assertTrue(e.getMessage().contains("saldo"));
        assertEquals(StatusConta.ATIVA, conta.getStatus());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
        verify(pixKeyRepository, never()).deleteAll(any());
    }

    @Test
    void recusaEncerrarContaJaEncerrada() {
        conta.setStatus(StatusConta.ENCERRADA);

        assertThrows(BusinessException.class, () -> service.encerrar("maria@exemplo.com", "123456"));
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }
}
