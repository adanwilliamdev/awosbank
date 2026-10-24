package com.awosbank.api.integration;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fluxos da API REST de ponta a ponta: segurança + controller + serviço + banco + auditoria. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String SENHA = "senha123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;
    @Autowired TransacaoService transacaoService;

    @Test
    void depositoESaqueAtualizamSaldoEGeramComprovante() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        mvc.perform(post("/api/deposito").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.comprovante").exists());

        mvc.perform(post("/api/saque").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 30.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void saqueAcimaDoSaldoRetorna400ComMensagem() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        mvc.perform(post("/api/saque").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensagem").value("Saldo insuficiente"));
    }

    @Test
    void corpoInvalidoRetorna400EmJson() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        mvc.perform(post("/api/deposito").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void transferenciaGeraComprovanteQueSoOsEnvolvidosConseguemVer() throws Exception {
        Usuario origem = TestData.novoUsuario(usuarioService, SENHA);
        Usuario destino = TestData.novoUsuario(usuarioService, SENHA);
        Usuario estranho = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(origem, new BigDecimal("500.00"));

        String corpo = "{\"numeroContaDestino\":\"" + destino.getConta().getNumeroConta()
                + "\",\"valor\":150.50,\"tipo\":\"PIX\",\"senha\":\"" + SENHA + "\"}";
        MvcResult res = mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andReturn();
        String codigo = JsonPath.read(res.getResponse().getContentAsString(), "$.comprovante");

        mvc.perform(get("/api/comprovante/" + codigo).with(user(origem.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(150.50))
                .andExpect(jsonPath("$.tipo").value("PIX"));
        mvc.perform(get("/api/comprovante/" + codigo).with(user(destino.getEmail())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/comprovante/" + codigo).with(user(estranho.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Sem acesso ao comprovante"));
    }

    @Test
    void transferenciaComSenhaErradaEhRecusada() throws Exception {
        Usuario origem = TestData.novoUsuario(usuarioService, SENHA);
        Usuario destino = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(origem, new BigDecimal("500.00"));

        String corpo = "{\"numeroContaDestino\":\"" + destino.getConta().getNumeroConta()
                + "\",\"valor\":10.00,\"tipo\":\"PIX\",\"senha\":\"errada\"}";
        mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Senha incorreta"));
    }

    @Test
    void chavesPixSaoCadastradasListadasERemovidas() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        MvcResult criada = mvc.perform(post("/api/pix/keys").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ALEATORIA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        String id = JsonPath.read(criada.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/pix/keys").with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(id)));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/pix/keys/" + id).with(user(u.getEmail())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void apiExigeAutenticacao() throws Exception {
        mvc.perform(get("/api/auditoria")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/pix/keys")).andExpect(status().is3xxRedirection());
    }

    @Test
    void auditoriaMostraAsOperacoesDoProprioUsuarioInclusiveAsQueFalharam() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);
        Usuario outro = TestData.novoUsuario(usuarioService, SENHA);

        mvc.perform(post("/api/deposito").with(user(u.getEmail())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 50.00}"));
        mvc.perform(post("/api/saque").with(user(u.getEmail())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 9999.00}"));
        mvc.perform(post("/api/deposito").with(user(outro.getEmail())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 1.00}"));

        // u tem 3 registros (CADASTRO + DEPOSITO + SAQUE); os do "outro" não aparecem
        mvc.perform(get("/api/auditoria").with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.itens[*].acao").value(hasItem("CADASTRO")))
                .andExpect(jsonPath("$.itens[*].acao").value(hasItem("DEPOSITO")))
                .andExpect(jsonPath("$.itens[*].acao").value(hasItem("SAQUE")))
                .andExpect(jsonPath("$.itens[?(@.sucesso == false)].detalhes").value(hasItem(containsString("Saldo insuficiente"))));
    }

    @Test
    void documentacaoOpenApiListaOsEndpointsDaApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AWOS Bank API"))
                .andExpect(jsonPath("$.paths['/api/transferencia']").exists())
                .andExpect(jsonPath("$.paths['/api/auditoria']").exists())
                .andExpect(jsonPath("$.paths['/api/pix/keys']").exists());
    }

    @Test
    void healthcheckEhPublicoEEstaUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
