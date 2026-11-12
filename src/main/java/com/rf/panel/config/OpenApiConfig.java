package com.rf.panel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Portada de Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8082}")
    private String puerto;

    @Bean
    public OpenAPI rfPanelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RF Inventario - Panel API")
                        .version("0.0.1")
                        .description("""
                                Backend del panel web de RF Inventario.

                                **Todo requiere token menos `/api/auth/login`, `/api/panel/auth/login`
                                y `/api/ping`.** Hay dos logins, con código + PIN cada uno:
                                `/api/auth/login` para el operario de la app Android (tabla `users`) y
                                `/api/panel/auth/login` para el administrador del panel web (tabla
                                `panel_admins`). Solo el token del panel abre `/api/operarios/*`.

                                El panel y la app Android comparten la misma base MySQL: la tabla
                                `products` es la fuente de verdad del inventario. Este servicio hace el
                                CRUD sobre ella; la app la sigue leyendo y escribiendo por JDBC directo.
                                """)
                        .contact(new Contact().name("German Celestino").email("germancelestino98@gmail.com"))
                        .license(new License().name("Uso interno")))
                .servers(List.of(
                        new Server().url("http://localhost:" + puerto).description("Local")
                ))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devuelto por POST /api/auth/login o /api/panel/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
