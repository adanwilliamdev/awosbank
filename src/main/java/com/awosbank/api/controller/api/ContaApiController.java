package com.awosbank.api.controller.api;

import com.awosbank.api.dto.EncerrarContaRequest;
import com.awosbank.api.dto.RespostaApi;
import com.awosbank.api.service.EncerramentoContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Conta", description = "Operações sobre a conta do cliente autenticado")
@RestController
@RequestMapping("/api/conta")
@RequiredArgsConstructor
public class ContaApiController {

    private final EncerramentoContaService encerramentoService;

    @Operation(summary = "Encerra (exclui) a conta do cliente",
            description = "Exige a senha do titular e saldo zerado. Remove o login e as chaves PIX, "
                    + "marca a conta como ENCERRADA e encerra a sessão.")
    @ApiResponse(responseCode = "200", description = "Conta encerrada")
    @ApiResponse(responseCode = "400", description = "Senha incorreta ou conta com saldo")
    @PostMapping("/encerrar")
    public ResponseEntity<RespostaApi> encerrar(@RequestBody EncerrarContaRequest body, Authentication auth,
                                                HttpServletRequest request, HttpServletResponse response) {
        encerramentoService.encerrar(auth.getName(), body.senha());
        // O usuário deixou de existir: derruba a sessão para não manter um login "fantasma".
        new SecurityContextLogoutHandler().logout(request, response, auth);
        return ResponseEntity.ok(RespostaApi.sucesso("Conta encerrada com sucesso."));
    }
}
