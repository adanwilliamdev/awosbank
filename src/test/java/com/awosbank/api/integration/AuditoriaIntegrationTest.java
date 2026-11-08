package com.awosbank.api.integration;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.AuditoriaRepository;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.AuditoriaService;
import com.awosbank.api.service.EncerramentoContaService;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Garante que a auditoria é gravada de verdade (sucesso, falha com rollback e logins). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaIntegrationTest {

    private static final String SENHA = "senha123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;
    @Autowired TransacaoService transacaoService;
    @Autowired EncerramentoContaService encerramentoContaService;
    @Autowired AuditoriaService auditoriaService;
    @Autowired AuditoriaRepository auditoriaRepository;

    private List<Auditoria> historico(Usuario u) {
        return auditoriaService.listarDoUsuario(u.getId(), PageRequest.of(0, 50)).getContent();
    }

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private TransferenciaDTO transferencia(Usuario destino, String valor, String senha) {
        TransferenciaDTO d = new TransferenciaDTO();
        d.setNumeroContaDestino(destino.getConta().getNumeroConta());
        d.setValor(new BigDecimal(valor));
        d.setTipo(TipoTransacao.PIX);
        d.setSenha(senha);
        return d;
    }

    @Test
    void cadastroDepositoETransferenciaFicamRegistrados() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);

        transacaoService.depositar(a, new BigDecimal("200.00"));
        var t = transacaoService.transferir(a, transferencia(b, "75.00", SENHA));

        List<Auditoria> registros = historico(a);
        assertEquals(List.of(AcaoAuditoria.TRANSFERENCIA, AcaoAuditoria.DEPOSITO, AcaoAuditoria.CADASTRO),
                registros.stream().map(Auditoria::getAcao).toList(), "mais recente primeiro");
        assertTrue(registros.stream().allMatch(Auditoria::isSucesso));
        assertTrue(registros.get(0).getDetalhes().contains("comprovante=" + t.getComprovanteCodigo()));
        assertTrue(registros.get(0).getDetalhes().contains("valor=75.00"));
    }

    @Test
    void operacaoQueFalhaEFazRollbackContinuaRegistradaSemVazarASenha() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);

        assertThrows(BusinessException.class,
                () -> transacaoService.transferir(a, transferencia(b, "10.00", "SENHA-ERRADA-123")));

        Auditoria falha = historico(a).get(0);
        assertEquals(AcaoAuditoria.TRANSFERENCIA, falha.getAcao());
        assertFalse(falha.isSucesso());
        assertTrue(falha.getDetalhes().contains("motivo=Senha incorreta"));
        assertFalse(falha.getDetalhes().contains("SENHA-ERRADA-123"));
        assertFalse(falha.getDetalhes().contains(SENHA));
    }

    @Test
    void loginComSucessoEComFalhaSaoRegistrados() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        mvc.perform(post("/login").with(csrf()).param("username", u.getEmail()).param("password", SENHA));
        mvc.perform(post("/login").with(csrf()).param("username", u.getEmail()).param("password", "errada"));

        assertEquals(1, auditoriaRepository.countByUsuarioEmailAndAcao(u.getEmail(), AcaoAuditoria.LOGIN_SUCESSO));
        assertEquals(1, auditoriaRepository.countByUsuarioEmailAndAcao(u.getEmail(), AcaoAuditoria.LOGIN_FALHA));
        Auditoria falha = historico(u).stream()
                .filter(a -> a.getAcao() == AcaoAuditoria.LOGIN_FALHA).findFirst().orElseThrow();
        assertFalse(falha.isSucesso());
        assertFalse(falha.getDetalhes().contains("errada"), "a senha digitada nunca é auditada");
    }

    @Test
    void quemReaproveitaOEmailDeUmaContaEncerradaNaoEnxergaOHistoricoAnterior() {
        Usuario antigo = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(antigo, new BigDecimal("50.00"));
        transacaoService.sacar(antigo, new BigDecimal("50.00"));

        // encerramento feito pelo próprio titular (o ator vem do contexto de segurança, como na requisição web)
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                antigo.getEmail(), "n/a", AuthorityUtils.createAuthorityList("ROLE_CLIENTE")));
        encerramentoContaService.encerrar(antigo.getEmail(), SENHA);
        SecurityContextHolder.clearContext();

        // outra pessoa passa a usar o mesmo e-mail
        CadastroDTO dto = new CadastroDTO();
        dto.setNomeCompleto("Novo Titular");
        dto.setCpfCnpj(antigo.getCpfCnpj());
        dto.setEmail(antigo.getEmail());
        dto.setSenha(SENHA);
        dto.setConfirmarSenha(SENHA);
        dto.setTipoConta(antigo.getConta().getTipoConta());
        Usuario novo = usuarioService.cadastrar(dto);

        assertNotEquals(antigo.getId(), novo.getId());
        assertEquals(List.of(AcaoAuditoria.CADASTRO), historico(novo).stream().map(Auditoria::getAcao).toList(),
                "o novo titular só vê o próprio histórico");

        // o registro do encerramento foi preservado, vinculado ao ID do titular antigo
        assertEquals(AcaoAuditoria.CONTA_ENCERRADA, historico(antigo).get(0).getAcao());
    }

    @Test
    void historicoDeUmUsuarioNuncaIncluiRegistrosDeOutro() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(b, new BigDecimal("10.00"));

        assertEquals(1, historico(a).size(), "só o CADASTRO de A");
        assertEquals(2, historico(b).size(), "CADASTRO + DEPOSITO de B");
    }

    @Test
    void listarComIdNuloNaoDevolveRegistrosSemDono() throws Exception {
        String email = "fantasma-" + System.nanoTime() + "@awosbank.test";
        mvc.perform(post("/login").with(csrf()).param("username", email).param("password", "qualquer"));
        assertEquals(1, auditoriaRepository.countByUsuarioEmailAndAcao(email, AcaoAuditoria.LOGIN_FALHA));

        assertTrue(auditoriaService.listarDoUsuario(null, PageRequest.of(0, 50)).isEmpty());
    }

    @Test
    void tentativaDeLoginComUsuarioInexistenteTambemEhRegistrada() throws Exception {
        String email = "fantasma-" + System.nanoTime() + "@awosbank.test";

        mvc.perform(post("/login").with(csrf()).param("username", email).param("password", "qualquer"));

        assertEquals(1, auditoriaRepository.countByUsuarioEmailAndAcao(email, AcaoAuditoria.LOGIN_FALHA));
    }
}
