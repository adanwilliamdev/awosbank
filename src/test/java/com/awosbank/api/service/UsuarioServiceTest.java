package com.awosbank.api.service;

import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.dto.PerfilDTO;
import com.awosbank.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock ContaService contaService;
    @Mock PasswordEncoder passwordEncoder;

    UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, contaService, passwordEncoder);
        when(passwordEncoder.encode("senha123")).thenReturn("HASH");
        when(contaService.criarConta(any())).thenReturn(
                Conta.builder().numeroConta("123456-7").agencia("0001").tipoConta(TipoConta.CORRENTE).build());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CadastroDTO cadastro() {
        CadastroDTO dto = new CadastroDTO();
        dto.setNomeCompleto("Maria da Silva");
        dto.setCpfCnpj("111.444.777-35");
        dto.setEmail("  Maria@Exemplo.COM ");
        dto.setSenha("senha123");
        dto.setConfirmarSenha("senha123");
        dto.setTipoConta(TipoConta.CORRENTE);
        return dto;
    }

    @Test
    void cadastraComEmailNormalizadoDocumentoLimpoESenhaCriptografada() {
        Usuario u = service.cadastrar(cadastro());

        assertEquals("maria@exemplo.com", u.getEmail());
        assertEquals("11144477735", u.getCpfCnpj());
        assertEquals("HASH", u.getSenha(), "a senha nunca é armazenada em texto puro");
        assertTrue(u.getRoles().contains("ROLE_CLIENTE"));
        assertEquals(StatusConta.ATIVA, u.getConta().getStatus());
    }

    @Test
    void rejeitaSenhasQueNaoConferem() {
        CadastroDTO dto = cadastro();
        dto.setConfirmarSenha("outra");
        assertEquals("Senhas não conferem", assertThrows(BusinessException.class, () -> service.cadastrar(dto)).getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejeitaDocumentoInvalido() {
        CadastroDTO dto = cadastro();
        dto.setCpfCnpj("11144477736");
        assertEquals("CPF/CNPJ inválido", assertThrows(BusinessException.class, () -> service.cadastrar(dto)).getMessage());
    }

    @Test
    void rejeitaEmailJaCadastrado() {
        when(usuarioRepository.existsByEmail("maria@exemplo.com")).thenReturn(true);
        assertEquals("Email já cadastrado",
                assertThrows(BusinessException.class, () -> service.cadastrar(cadastro())).getMessage());
        verify(contaService, never()).criarConta(any());
    }

    @Test
    void rejeitaDocumentoJaCadastrado() {
        when(usuarioRepository.existsByCpfCnpj("11144477735")).thenReturn(true);
        assertEquals("CPF/CNPJ já cadastrado",
                assertThrows(BusinessException.class, () -> service.cadastrar(cadastro())).getMessage());
    }

    @Test
    void buscarPorEmailIgnoraMaiusculasEFalhaQuandoNaoExiste() {
        Usuario u = Usuario.builder().email("a@b.com").build();
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        assertSame(u, service.buscarPorEmail(" A@B.com "));
        assertThrows(BusinessException.class, () -> service.buscarPorEmail("nao@existe.com"));
    }

    @Test
    void atualizarPerfilTrocaDadosENormalizaEmail() {
        Usuario atual = Usuario.builder().email("velho@exemplo.com").nomeCompleto("Antigo").build();
        when(usuarioRepository.findByEmail("velho@exemplo.com")).thenReturn(Optional.of(atual));

        PerfilDTO dto = new PerfilDTO();
        dto.setNomeCompleto("Nome Novo");
        dto.setEmail("Novo@Exemplo.com");
        dto.setTelefone("11999998888");

        Usuario salvo = service.atualizarPerfil("velho@exemplo.com", dto);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertSame(salvo, captor.getValue());
        assertEquals("novo@exemplo.com", salvo.getEmail());
        assertEquals("Nome Novo", salvo.getNomeCompleto());
        assertEquals("11999998888", salvo.getTelefone());
    }

    @Test
    void atualizarPerfilRejeitaEmailDeOutroUsuario() {
        Usuario atual = Usuario.builder().email("velho@exemplo.com").build();
        when(usuarioRepository.findByEmail("velho@exemplo.com")).thenReturn(Optional.of(atual));
        when(usuarioRepository.existsByEmail("ocupado@exemplo.com")).thenReturn(true);

        PerfilDTO dto = new PerfilDTO();
        dto.setNomeCompleto("Fulano");
        dto.setEmail("ocupado@exemplo.com");

        assertEquals("Novo email já cadastrado",
                assertThrows(BusinessException.class, () -> service.atualizarPerfil("velho@exemplo.com", dto)).getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void atualizarPerfilMantendoOMesmoEmailNaoConsultaDuplicidade() {
        Usuario atual = Usuario.builder().email("mesmo@exemplo.com").build();
        when(usuarioRepository.findByEmail("mesmo@exemplo.com")).thenReturn(Optional.of(atual));

        PerfilDTO dto = new PerfilDTO();
        dto.setNomeCompleto("Fulano");
        dto.setEmail("MESMO@exemplo.com");

        service.atualizarPerfil("mesmo@exemplo.com", dto);
        verify(usuarioRepository, never()).existsByEmail(any());
    }

    @Test
    void normalizarEmailTrataNulo() {
        assertNull(UsuarioService.normalizarEmail(null));
        assertEquals("a@b.com", UsuarioService.normalizarEmail("  A@B.COM "));
    }
}
