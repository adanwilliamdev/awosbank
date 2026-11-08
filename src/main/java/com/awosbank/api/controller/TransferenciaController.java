package com.awosbank.api.controller;

import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/transferencia")
public class TransferenciaController {

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

    @GetMapping
    public String form(Model model, Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        model.addAttribute("usuario", u);
        model.addAttribute("conta", u.getConta());
        if (!model.containsAttribute("transferenciaDTO")) {
            TransferenciaDTO novo = new TransferenciaDTO();
            novo.setChaveIdempotencia(UUID.randomUUID().toString()); // token novo a cada abertura do formulário
            model.addAttribute("transferenciaDTO", novo);
        }
        return "transferencia";
    }

    @PostMapping
    public String transferir(@Valid @ModelAttribute("transferenciaDTO") TransferenciaDTO dto,
                             BindingResult result,
                             Authentication auth,
                             Model model,
                             RedirectAttributes ra) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        if (dto.getChaveIdempotencia() == null || dto.getChaveIdempotencia().isBlank()) {
            // Formulário antigo em cache / token ausente: gera um para que um reenvio da re-renderização seja protegido.
            dto.setChaveIdempotencia(UUID.randomUUID().toString());
        }
        if (result.hasErrors()) {
            model.addAttribute("usuario", u);
            model.addAttribute("conta", u.getConta());
            return "transferencia";
        }
        try {
            Transacao t = transacaoService.transferir(u, dto);
            ra.addFlashAttribute("sucesso", "Transferência realizada!");
            return "redirect:/comprovante/" + t.getComprovanteCodigo();
        } catch (BusinessException e) {
            model.addAttribute("usuario", u);
            model.addAttribute("conta", u.getConta());
            model.addAttribute("erro", e.getMessage());
            return "transferencia";
        }
    }
}