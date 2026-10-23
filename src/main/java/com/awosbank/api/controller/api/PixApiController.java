package com.awosbank.api.controller.api;

import com.awosbank.api.domain.model.PixKey;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.PixKeyDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.PixKeyService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pix")
@RequiredArgsConstructor
public class PixApiController {

    private final UsuarioService usuarioService;
    private final PixKeyService pixService;

    @GetMapping("/keys")
    public List<Map<String, Object>> listar(Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        return pixService.listar(u).stream().map(k -> Map.<String, Object>of(
                "id", k.getId(),
                "tipo", k.getTipo().name(),
                "tipoDescricao", k.getTipo().getDescricao(),
                "chave", k.getChave(),
                "dataCriacao", k.getDataCriacao().toString()
        )).toList();
    }

    @PostMapping("/keys")
    public ResponseEntity<?> adicionar(@RequestBody PixKeyDTO dto, Authentication auth) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            PixKey k = pixService.adicionar(u, dto);
            return ResponseEntity.ok(Map.of("ok", true, "mensagem", "Chave PIX cadastrada!",
                    "id", k.getId()));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensagem", e.getMessage()));
        }
    }

    @DeleteMapping("/keys/{id}")
    public ResponseEntity<?> remover(@PathVariable UUID id, Authentication auth) {
        try {
            Usuario u = usuarioService.buscarPorEmail(auth.getName());
            pixService.remover(u, id);
            return ResponseEntity.ok(Map.of("ok", true, "mensagem", "Chave removida"));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensagem", e.getMessage()));
        }
    }
}