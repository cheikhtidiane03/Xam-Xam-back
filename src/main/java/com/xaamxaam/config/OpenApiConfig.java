package com.xaamxaam.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger / OpenAPI (springdoc).
 * Une fois l'application lancee, la documentation interactive est
 * disponible sur : http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_JWT = "bearerAuth";

    @Bean
    public OpenAPI xaamXaamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Xaam-Xaam+ API")
                        .description("API du backend de la plateforme d'accompagnement scolaire par IA guidee "
                                + "(methode socratique). Documentation interactive pour tester tous les endpoints "
                                + "par role : Eleve, Parent, Enseignant, AdminEtablissement, Superadmin.")
                        .version("v0.1.0")
                        .contact(new Contact().name("Xaam-Xaam+ Team")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_JWT))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_JWT, new SecurityScheme()
                                .name(SCHEME_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Coller le token JWT obtenu via /api/auth/login (sans le prefixe 'Bearer ')")));
    }
}
