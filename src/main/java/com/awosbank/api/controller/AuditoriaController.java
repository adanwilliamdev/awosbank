package com.awosbank.api.controller;

import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.service.AuditoriaService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Tela "Atividade da conta": o cliente consulta o próprio histórico de auditoria (logins, operações, falhas). */
@Controller
@RequiredArgsConstructor
public class AuditoriaController {

    private static final int TAMANHO_PAGINA = 15;

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    @GetMapping("/auditoria")
    public String historico(@RequestParam(defaultValue = "0") int pagina,
                            @RequestParam(defaultValue = "todos") String resultado,
                            Authentication auth,
                            Model model) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        boolean apenasFalhas = "falhas".equalsIgnoreCase(resultado);

        Page<Auditoria> registros = auditoriaService.listarDoUsuario(
                u.getId(), apenasFalhas, PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA));

        model.addAttribute("usuario", u);
        model.addAttribute("registros", registros);
        model.addAttribute("resultado", apenasFalhas ? "falhas" : "todos");
        return "auditoria";
    }
}
