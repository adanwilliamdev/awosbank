package com.awosbank.api.controller.api;

import com.awosbank.api.domain.model.PixKey;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.ChavePixResposta;
import com.awosbank.api.dto.PixKeyDTO;
import com.awosbank.api.dto.RespostaApi;
import com.awosbank.api.service.PixKeyService;
import com.awosbank.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chaves PIX", description = "Cadastro, consulta e remoção das chaves PIX do cliente autenticado")
@RestController
@RequestMapping("/api/pix")
@RequiredArgsConstructor
public class PixApiController {

    private final UsuarioService usuarioService;
    private final PixKeyService pixService;

    @Operation(summary = "Lista as chaves PIX do cliente")
    @GetMapping("/keys")
    public List<ChavePixResposta> listar(Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        return pixService.listar(u).stream().map(k -> new ChavePixResposta(
                k.getId(),
                k.getTipo().name(),
                k.getTipo().getDescricao(),
                k.getChave(),
                k.getDataCriacao().toString()
        )).toList();
    }

    @Operation(summary = "Cadastra uma chave PIX",
            description = "Máximo de 5 chaves por conta. Para o tipo ALEATORIA, deixe `chave` em branco para o servidor gerar um UUID.")
    @ApiResponse(responseCode = "200", description = "Chave cadastrada")
    @ApiResponse(responseCode = "400", description = "Chave inválida, duplicada ou limite atingido")
    @PostMapping("/keys")
    public ResponseEntity<RespostaApi> adicionar(@RequestBody PixKeyDTO dto, Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        PixKey k = pixService.adicionar(u, dto);
        return ResponseEntity.ok(RespostaApi.criado("Chave PIX cadastrada!", k.getId()));
    }

    @Operation(summary = "Remove uma chave PIX do cliente")
    @ApiResponse(responseCode = "200", description = "Chave removida")
    @ApiResponse(responseCode = "400", description = "Chave inexistente ou de outro cliente")
    @DeleteMapping("/keys/{id}")
    public ResponseEntity<RespostaApi> remover(@PathVariable UUID id, Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        pixService.remover(u, id);
        return ResponseEntity.ok(RespostaApi.sucesso("Chave removida"));
    }
}
