package com.awosbank.api.controller.api;

import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.ComprovanteResposta;
import com.awosbank.api.dto.RespostaApi;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.dto.ValorRequest;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transações", description = "Transferências, depósitos, saques e comprovantes")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransacaoApiController {

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

    @Operation(summary = "Realiza uma transferência (PIX ou TED)",
            description = "O destino pode ser uma chave PIX ou um número de conta. A senha do cliente é exigida para confirmar.")
    @ApiResponse(responseCode = "200", description = "Transferência realizada")
    @ApiResponse(responseCode = "400", description = "Saldo insuficiente, senha incorreta, destino inexistente, etc.")
    @PostMapping("/transferencia")
    public ResponseEntity<RespostaApi> transferir(@RequestBody TransferenciaDTO dto, Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        Transacao t = transacaoService.transferirPorIdentificador(u, dto);
        return ResponseEntity.ok(RespostaApi.comprovante("Transferência realizada com sucesso!", t.getComprovanteCodigo()));
    }

    @Operation(summary = "Realiza um depósito na conta do cliente")
    @ApiResponse(responseCode = "200", description = "Depósito realizado")
    @ApiResponse(responseCode = "400", description = "Valor inválido ou conta inativa")
    @PostMapping("/deposito")
    public ResponseEntity<RespostaApi> depositar(@RequestBody ValorRequest body, Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        Transacao t = transacaoService.depositar(u, body.valor());
        return ResponseEntity.ok(RespostaApi.comprovante("Depósito realizado!", t.getComprovanteCodigo()));
    }

    @Operation(summary = "Realiza um saque na conta do cliente")
    @ApiResponse(responseCode = "200", description = "Saque realizado")
    @ApiResponse(responseCode = "400", description = "Valor inválido, saldo insuficiente ou conta inativa")
    @PostMapping("/saque")
    public ResponseEntity<RespostaApi> sacar(@RequestBody ValorRequest body, Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        Transacao t = transacaoService.sacar(u, body.valor());
        return ResponseEntity.ok(RespostaApi.comprovante("Saque realizado!", t.getComprovanteCodigo()));
    }

    @Operation(summary = "Consulta um comprovante",
            description = "Só retorna comprovantes de transações em que o cliente é origem ou destino.")
    @ApiResponse(responseCode = "200", description = "Comprovante encontrado")
    @ApiResponse(responseCode = "400", description = "Comprovante inexistente ou sem acesso")
    @GetMapping("/comprovante/{codigo}")
    public ResponseEntity<ComprovanteResposta> comprovante(@PathVariable String codigo, Authentication auth) {
        Usuario u = usuarioService.buscarPorEmail(auth.getName());
        Transacao t = transacaoService.buscarPorComprovante(codigo);
        boolean pertence = (t.getContaOrigem() != null && t.getContaOrigem().getId().equals(u.getConta().getId()))
                || (t.getContaDestino() != null && t.getContaDestino().getId().equals(u.getConta().getId()));
        if (!pertence) throw new BusinessException("Sem acesso ao comprovante");

        return ResponseEntity.ok(new ComprovanteResposta(
                true,
                t.getComprovanteCodigo(),
                t.getTipo().getDescricao(),
                t.getValor(),
                t.getDataHora().toString(),
                t.getContaOrigem() == null ? null
                        : t.getContaOrigem().getAgencia() + " / " + t.getContaOrigem().getNumeroConta(),
                t.getContaDestino() == null ? null
                        : t.getContaDestino().getAgencia() + " / " + t.getContaDestino().getNumeroConta()));
    }
}
