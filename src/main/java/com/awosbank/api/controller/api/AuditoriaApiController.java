package com.awosbank.api.controller.api;

import com.awosbank.api.dto.AuditoriaResposta;
import com.awosbank.api.dto.PaginaAuditoria;
import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auditoria", description = "Histórico de auditoria das operações do cliente autenticado")
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaApiController {

    private static final int TAMANHO_MAXIMO = 100;

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Lista o histórico de auditoria do cliente",
            description = "Logins, transferências, depósitos, saques, alterações de perfil e chaves PIX — "
                    + "inclusive as tentativas que falharam. Do mais recente para o mais antigo.")
    @GetMapping
    public PaginaAuditoria listar(
            @Parameter(description = "Página (começa em 0)") @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Itens por página (máx. 100)") @RequestParam(defaultValue = "20") int tamanho,
            Authentication auth) {
        int p = Math.max(pagina, 0);
        int t = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);
        Page<Auditoria> page = auditoriaService.listarDoUsuario(auth.getName(), PageRequest.of(p, t));
        return PaginaAuditoria.de(page, page.getContent().stream().map(AuditoriaResposta::de).toList());
    }
}
