package com.awosbank.api.integration;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.dto.PerfilDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corrida real entre requisições: várias threads passam juntas pelo {@code existsBy...} e só o banco
 * (constraint UNIQUE) consegue decidir quem ganha. Quem perde tem de receber erro de NEGÓCIO — nunca a
 * página de "erro inesperado" — e não pode deixar conta órfã.
 *
 * <p>As mensagens exatas por tipo de banco são conferidas em {@code PostgresMigrationTest}; aqui (H2) o
 * foco é: um vencedor, os demais recusados como {@link BusinessException}, nada de lixo no banco.
 */
@SpringBootTest
@ActiveProfiles("test")
class CadastroConcorrenteTest {

    private static final String SENHA = "senha123";
    private static final int CONCORRENTES = 6;

    @Autowired UsuarioService usuarioService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ContaRepository contaRepository;

    private static String descrever(List<Object> resultados) {
        return resultados.stream().map(o -> o instanceof Throwable t ? t.getClass().getSimpleName() + ": " + t.getMessage()
                : "OK").toList().toString();
    }

    private void assertUmVencedorEOsDemaisRecusadosComoNegocio(List<Object> resultados) {
        assertEquals(1, resultados.stream().filter(o -> o instanceof Usuario).count(), descrever(resultados));
        for (Object r : resultados) {
            if (r instanceof Throwable t) {
                assertInstanceOf(BusinessException.class, t, "esperado erro de negócio, veio: " + descrever(resultados));
            }
        }
    }

    @Test
    void cadastrosSimultaneosComOMesmoEmailGeramUmUsuarioEOsDemaisSaoRecusados() throws Exception {
        String email = "corrida-" + System.nanoTime() + "@awosbank.test";
        List<CadastroDTO> dtos = new ArrayList<>();
        for (int i = 0; i < CONCORRENTES; i++) {
            int semente = TestData.proximaSemente();
            dtos.add(TestData.cadastroDto(semente, email, TestData.cpfValido(semente), SENHA));
        }
        long contasAntes = contaRepository.count();

        List<Object> resultados = TestData.cadastrarEmParalelo(usuarioService, dtos);

        assertUmVencedorEOsDemaisRecusadosComoNegocio(resultados);
        assertTrue(usuarioRepository.existsByEmail(email));
        assertEquals(contasAntes + 1, contaRepository.count(),
                "quem perdeu a corrida não pode deixar uma conta órfã (a transação inteira faz rollback)");
    }

    @Test
    void cadastrosSimultaneosComOMesmoDocumentoGeramUmUsuarioEOsDemaisSaoRecusados() throws Exception {
        int semente = TestData.proximaSemente();
        String cpf = TestData.cpfValido(semente);
        List<CadastroDTO> dtos = new ArrayList<>();
        for (int i = 0; i < CONCORRENTES; i++) {
            int s = TestData.proximaSemente();
            dtos.add(TestData.cadastroDto(s, "cpf-corrida-" + s + "-" + System.nanoTime() + "@awosbank.test", cpf, SENHA));
        }
        long contasAntes = contaRepository.count();

        List<Object> resultados = TestData.cadastrarEmParalelo(usuarioService, dtos);

        assertUmVencedorEOsDemaisRecusadosComoNegocio(resultados);
        assertTrue(usuarioRepository.existsByCpfCnpj(cpf));
        assertEquals(contasAntes + 1, contaRepository.count());
    }

    @Test
    void trocasSimultaneasParaOMesmoEmailTerminamComUmVencedorSemErroInesperado() throws Exception {
        List<Usuario> usuarios = new ArrayList<>();
        for (int i = 0; i < CONCORRENTES; i++) {
            usuarios.add(TestData.novoUsuario(usuarioService, SENHA));
        }
        String disputado = "disputado-" + System.nanoTime() + "@awosbank.test";

        ExecutorService pool = Executors.newFixedThreadPool(CONCORRENTES);
        try {
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Object>> futuros = new ArrayList<>();
            for (Usuario u : usuarios) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    PerfilDTO dto = new PerfilDTO();
                    dto.setNomeCompleto("Nome Novo");
                    dto.setEmail(disputado);
                    try {
                        return usuarioService.atualizarPerfil(u.getEmail(), dto);
                    } catch (Throwable erro) {
                        return erro;
                    }
                }));
            }
            largada.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }

            assertUmVencedorEOsDemaisRecusadosComoNegocio(resultados);
            assertTrue(usuarioRepository.existsByEmail(disputado));
        } finally {
            pool.shutdownNow();
        }
    }
}
