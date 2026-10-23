package com.awosbank.api.controller;

import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.PerfilDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String form(Authentication auth, Model model) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        PerfilDTO dto = new PerfilDTO();
        dto.setNomeCompleto(u.getNomeCompleto());
        dto.setEmail(u.getEmail());
        dto.setTelefone(u.getTelefone());
        dto.setDataNascimento(u.getDataNascimento());
        dto.setEndereco(u.getEndereco());

        model.addAttribute("usuario", u);
        model.addAttribute("perfilDTO", dto);
        return "perfil";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("perfilDTO") PerfilDTO dto,
                         BindingResult result,
                         Authentication auth, Model model,
                         RedirectAttributes ra) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        if (result.hasErrors()) {
            model.addAttribute("usuario", u);
            return "perfil";
        }
        try {
            usuarioService.atualizarPerfil(auth.getName(), dto);
            ra.addFlashAttribute("sucesso", "Perfil atualizado!");
            return "redirect:/perfil";
        } catch (BusinessException e) {
            model.addAttribute("usuario", u);
            model.addAttribute("erro", e.getMessage());
            return "perfil";
        }
    }
}