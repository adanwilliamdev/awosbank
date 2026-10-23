package com.awosbank.api.controller;

import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.ExtratoFiltroDTO;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class ExtratoController {

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

    @GetMapping("/extrato")
    public String extrato(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String tipo,
            Authentication auth,
            Model model) {

        Usuario u = usuarioService.buscarPorEmail(auth.getName());

        ExtratoFiltroDTO filtro = new ExtratoFiltroDTO();
        filtro.setDataInicio(dataInicio != null ? dataInicio : LocalDate.now().minusDays(30));
        filtro.setDataFim(dataFim != null ? dataFim : LocalDate.now());
        filtro.setTipo(tipo);

        model.addAttribute("usuario", u);
        model.addAttribute("conta", u.getConta());
        model.addAttribute("filtro", filtro);
        model.addAttribute("tipos", TipoTransacao.values());
        model.addAttribute("transacoes", transacaoService.buscarExtrato(u.getConta().getId(), filtro));
        return "extrato";
    }
}