package com.awosbank.api.controller;

import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String formCadastro(Model model) {
        model.addAttribute("cadastroDTO", new CadastroDTO());
        model.addAttribute("tiposConta", TipoConta.values());
        return "auth/cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastroDTO") CadastroDTO dto,
                            BindingResult result,
                            Model model,
                            RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("tiposConta", TipoConta.values());
            return "auth/cadastro";
        }
        try {
            usuarioService.cadastrar(dto);
            ra.addFlashAttribute("sucesso", "Conta criada com sucesso! Faça login.");
            return "redirect:/login";
        } catch (BusinessException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("tiposConta", TipoConta.values());
            return "auth/cadastro";
        }
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}