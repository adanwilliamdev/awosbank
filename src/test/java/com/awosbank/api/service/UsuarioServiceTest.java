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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
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
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
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
        verify(usuarioRepository, never()).saveAndFlush(any());
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
        verify(usuarioRepository).saveAndFlush(captor.capture());
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
        verify(usuarioRepository, never()).saveAndFlush(any());
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

    // ---------- corrida: a constraint UNIQUE do banco decide ----------

    private static final String PG_EMAIL = "ERROR: duplicate key value violates unique constraint \"uk_usuarios_email\"\n"
            + "  Detail: Key (email)=(maria@exemplo.com) already exists.";
    private static final String PG_EMAIL_COM_CPF_NO_VALOR = "ERROR: duplicate key value violates unique constraint \"uk_usuarios_email\"\n"
            + "  Detail: Key (email)=(cpf@exemplo.com) already exists.";
    private static final String PG_CPF = "ERROR: duplicate key value violates unique constraint \"uk_usuarios_cpf_cnpj\"\n"
            + "  Detail: Key (cpf_cnpj)=(11144477735) already exists.";
    private static final String H2_EMAIL = "Unique index or primary key violation: "
            + "\"PUBLIC.UK_6DOTKOTT2KJSP8VW4D0M25FB7_INDEX_4 ON PUBLIC.USUARIOS(EMAIL NULLS FIRST) VALUES ( /* 1 */ 'maria@exemplo.com' )\"";
    private static final String H2_CPF = "Unique index or primary key violation: "
            + "\"PUBLIC.UK_X_INDEX_4 ON PUBLIC.USUARIOS(CPF_CNPJ NULLS FIRST) VALUES ( /* 1 */ '11144477735' )\"";
    private static final String PG_NUMERO_CONTA = "ERROR: duplicate key value violates unique constraint \"uk_contas_numero\"\n"
            + "  Detail: Key (numero_conta)=(123456-7) already exists.";

    /** Como o Hibernate/Spring entregam: DataIntegrityViolationException -> ... -> SQLException com SQLSTATE. */
    private static DataIntegrityViolationException violacao(String sqlState, String mensagemDoBanco) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLException(mensagemDoBanco, sqlState));
    }

    private void cadastroFalhaNoFlushCom(DataIntegrityViolationException erro) {
        doThrow(erro).when(usuarioRepository).saveAndFlush(any(Usuario.class));
    }

    @Test
    void corridaNoCadastroComMesmoEmailViraErroDeNegocio() {
        for (String mensagem : new String[]{PG_EMAIL, H2_EMAIL, PG_EMAIL_COM_CPF_NO_VALOR}) {
            cadastroFalhaNoFlushCom(violacao("23505", mensagem));
            assertEquals("Email já cadastrado",
                    assertThrows(BusinessException.class, () -> service.cadastrar(cadastro())).getMessage(), mensagem);
        }
    }

    @Test
    void corridaNoCadastroComMesmoDocumentoViraErroDeNegocio() {
        for (String mensagem : new String[]{PG_CPF, H2_CPF}) {
            cadastroFalhaNoFlushCom(violacao("23505", mensagem));
            assertEquals("CPF/CNPJ já cadastrado",
                    assertThrows(BusinessException.class, () -> service.cadastrar(cadastro())).getMessage(), mensagem);
        }
    }

    @Test
    void colisaoDeUnicidadeEmOutraColunaPedeParaTentarDeNovoSemVazarDetalhes() {
        cadastroFalhaNoFlushCom(violacao("23505", PG_NUMERO_CONTA));

        BusinessException erro = assertThrows(BusinessException.class, () -> service.cadastrar(cadastro()));

        assertEquals("Não foi possível concluir o cadastro agora. Tente novamente.", erro.getMessage());
        assertFalse(erro.getMessage().contains("numero_conta"));
    }

    @Test
    void cadastroNaoEngoleViolacoesQueNaoSaoDeUnicidade() {
        // NOT NULL na coluna email cita "email" na mensagem, mas é um bug e deve subir para virar log com código.
        DataIntegrityViolationException notNull = violacao("23502",
                "null value in column \"email\" of relation \"usuarios\" violates not-null constraint");
        cadastroFalhaNoFlushCom(notNull);

        assertSame(notNull, assertThrows(DataIntegrityViolationException.class, () -> service.cadastrar(cadastro())));
    }

    @Test
    void corridaNaTrocaDeEmailViraErroDeNegocio() {
        Usuario atual = Usuario.builder().email("velho@exemplo.com").build();
        when(usuarioRepository.findByEmail("velho@exemplo.com")).thenReturn(Optional.of(atual));
        PerfilDTO dto = new PerfilDTO();
        dto.setNomeCompleto("Fulano");
        dto.setEmail("disputado@exemplo.com");

        for (String mensagem : new String[]{PG_EMAIL, H2_EMAIL}) {
            doThrow(violacao("23505", mensagem)).when(usuarioRepository).saveAndFlush(any(Usuario.class));
            assertEquals("Novo email já cadastrado",
                    assertThrows(BusinessException.class, () -> service.atualizarPerfil("velho@exemplo.com", dto)).getMessage());
        }
    }

    @Test
    void trocaDeEmailNaoEngoleViolacoesQueNaoSaoDeUnicidade() {
        Usuario atual = Usuario.builder().email("velho@exemplo.com").build();
        when(usuarioRepository.findByEmail("velho@exemplo.com")).thenReturn(Optional.of(atual));
        PerfilDTO dto = new PerfilDTO();
        dto.setNomeCompleto("Fulano");
        dto.setEmail("novo@exemplo.com");
        DataIntegrityViolationException tamanho = violacao("22001", "value too long for type character varying(120)");
        doThrow(tamanho).when(usuarioRepository).saveAndFlush(any(Usuario.class));

        assertSame(tamanho, assertThrows(DataIntegrityViolationException.class,
                () -> service.atualizarPerfil("velho@exemplo.com", dto)));
    }

    @Test
    void normalizarEmailTrataNulo() {
        assertNull(UsuarioService.normalizarEmail(null));
        assertEquals("a@b.com", UsuarioService.normalizarEmail("  A@B.COM "));
    }
}
