package com.awosbank.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados do Swagger/OpenAPI. A UI fica em /swagger-ui.html e o JSON em /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    private static final String SESSAO = "sessao";

    @Bean
    public OpenAPI awosBankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AWOS Bank API")
                        .version("1.0.0")
                        .description("""
                                API REST do AWOS Bank (internet banking didático).

                                **Autenticação:** a API usa a sessão do login web. Entre em `/login` no mesmo navegador \
                                e depois use "Try it out" aqui. Requisições que alteram dados exigem o cabeçalho \
                                `X-XSRF-TOKEN`, que o Swagger UI preenche sozinho a partir do cookie `XSRF-TOKEN`.""")
                        .contact(new Contact().name("Adan William Oliveira Santos")
                                .url("https://github.com/adanwilliamdev")))
                .components(new Components().addSecuritySchemes(SESSAO,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Cookie de sessão criado pelo login em /login")))
                .addSecurityItem(new SecurityRequirement().addList(SESSAO));
    }
}
