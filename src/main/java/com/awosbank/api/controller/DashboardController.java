package com.awosbank.api.controller;

import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        model.addAttribute("usuario", u);
        model.addAttribute("conta", u.getConta());
        model.addAttribute("ultimas", transacaoService.ultimasTransacoes(u.getConta().getId(), 5));
        return "dashboard";
    }

    @PostMapping("/deposito")
    public String depositar(@RequestParam BigDecimal valor, Authentication auth, RedirectAttributes ra) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            transacaoService.depositar(u, valor);
            ra.addFlashAttribute("sucesso", "Depósito realizado!");
        } catch (BusinessException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/saque")
    public String sacar(@RequestParam BigDecimal valor, Authentication auth, RedirectAttributes ra) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            transacaoService.sacar(u, valor);
            ra.addFlashAttribute("sucesso", "Saque realizado!");
        } catch (BusinessException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/dashboard";
    }
}