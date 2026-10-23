package com.awosbank.api.controller;

import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ComprovanteController {

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

    @GetMapping("/comprovante/{codigo}")
    public String comprovante(@PathVariable String codigo, Authentication auth, Model model) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        Transacao t = transacaoService.buscarPorComprovante(codigo);

        boolean pertence = (t.getContaOrigem() != null && t.getContaOrigem().getId().equals(u.getConta().getId()))
                || (t.getContaDestino() != null && t.getContaDestino().getId().equals(u.getConta().getId()));
        if (!pertence) {
            throw new BusinessException("Comprovante não pertence ao usuário");
        }
        model.addAttribute("usuario", u);
        model.addAttribute("t", t);
        return "comprovante";
    }
}