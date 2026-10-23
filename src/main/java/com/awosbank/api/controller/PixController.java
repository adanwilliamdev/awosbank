package com.awosbank.api.controller;

import com.awosbank.api.domain.enums.TipoChavePix;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.service.PixKeyService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pix")
@RequiredArgsConstructor
public class PixController {

    private final UsuarioService usuarioService;
    private final PixKeyService pixService;

    @GetMapping
    public String pix(Authentication auth, Model model) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        model.addAttribute("usuario", u);
        model.addAttribute("conta", u.getConta());
        model.addAttribute("tipos", TipoChavePix.values());
        model.addAttribute("chaves", pixService.listar(u));
        return "pix";
    }
}