package com.awosbank.api.controller.api;

import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransacaoApiController {

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

    @PostMapping("/transferencia")
    public ResponseEntity<?> transferir(@RequestBody TransferenciaDTO dto, Authentication auth) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            Transacao t = transacaoService.transferirPorIdentificador(u, dto);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "mensagem", "Transferência realizada com sucesso!",
                    "comprovante", t.getComprovanteCodigo()
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensagem", e.getMessage()));
        }
    }

    @PostMapping("/deposito")
    public ResponseEntity<?> depositar(@RequestBody Map<String, BigDecimal> body, Authentication auth) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            Transacao t = transacaoService.depositar(u, body.get("valor"));
            return ResponseEntity.ok(Map.of("ok", true, "mensagem", "Depósito realizado!",
                    "comprovante", t.getComprovanteCodigo()));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensagem", e.getMessage()));
        }
    }

    @PostMapping("/saque")
    public ResponseEntity<?> sacar(@RequestBody Map<String, BigDecimal> body, Authentication auth) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            Transacao t = transacaoService.sacar(u, body.get("valor"));
            return ResponseEntity.ok(Map.of("ok", true, "mensagem", "Saque realizado!",
                    "comprovante", t.getComprovanteCodigo()));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensagem", e.getMessage()));
        }
    }

    @GetMapping("/comprovante/{codigo}")
    public ResponseEntity<?> comprovante(@PathVariable String codigo, Authentication auth) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            Transacao t = transacaoService.buscarPorComprovante(codigo);
            boolean pertence = (t.getContaOrigem() != null && t.getContaOrigem().getId().equals(u.getConta().getId()))
                    || (t.getContaDestino() != null && t.getContaDestino().getId().equals(u.getConta().getId()));
            if (!pertence) throw new BusinessException("Sem acesso ao comprovante");

            var m = new java.util.HashMap<String, Object>();
            m.put("ok", true);
            m.put("codigo", t.getComprovanteCodigo());
            m.put("tipo", t.getTipo().getDescricao());
            m.put("valor", t.getValor());
            m.put("dataHora", t.getDataHora().toString());
            m.put("origem", t.getContaOrigem() == null ? null :
                    t.getContaOrigem().getAgencia() + " / " + t.getContaOrigem().getNumeroConta());
            m.put("destino", t.getContaDestino() == null ? null :
                    t.getContaDestino().getAgencia() + " / " + t.getContaDestino().getNumeroConta());
            return ResponseEntity.ok(m);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensagem", e.getMessage()));
        }
    }
}