package com.awosbank.api.controller.api;

import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.NotificacaoResposta;
import com.awosbank.api.dto.TransacaoView;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notificações", description = "Movimentações recentes exibidas no sino da barra superior")
@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoApiController {

    private static final int LIMITE = 10;

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

    @Operation(summary = "Lista as últimas movimentações da conta como notificações")
    @GetMapping
    public List<NotificacaoResposta> listar(Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        return transacaoService.ultimasTransacoes(u.getConta().getId(), LIMITE).stream()
                .map(this::paraNotificacao)
                .toList();
    }

    private NotificacaoResposta paraNotificacao(TransacaoView v) {
        Transacao t = v.transacao();
        String titulo = switch (t.getTipo()) {
            case DEPOSITO -> "Depósito realizado";
            case SAQUE -> "Saque realizado";
            default -> (v.entrada() ? "Você recebeu " : "Você enviou ") + t.getTipo().getDescricao();
        };
        return new NotificacaoResposta(
                titulo,
                v.descricao(),
                t.getTipo() == TipoTransacao.DEPOSITO || t.getTipo() == TipoTransacao.SAQUE
                        ? t.getTipo().name() : "TRANSFERENCIA",
                v.entrada(),
                t.getValor(),
                t.getDataHora().toString(),
                t.getComprovanteCodigo());
    }
}
