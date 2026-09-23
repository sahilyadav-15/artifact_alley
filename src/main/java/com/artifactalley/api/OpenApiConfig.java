package com.artifactalley.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI artifactAlleyOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Artifact Alley REST API").version("v1")
                        .description("Auction API using the same transactional services as the JSP application. "
                                + "Protected calls require a JSESSIONID cookie obtained through POST /login. "
                                + "Fetch GET /api/v1/session/csrf first and send X-CSRF-Token on authenticated unsafe requests. "
                                + "Interactive OpenAPI is disabled by the production profile; operators use the detail-free "
                                + "/actuator/health/liveness and /actuator/health/readiness probes."))
                .components(new Components().addSecuritySchemes("sessionCookie",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID").description("Artifact Alley HttpSession cookie"))
                        .addSecuritySchemes("csrfHeader", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-CSRF-Token")
                                .description("Token returned by GET /api/v1/session/csrf")));
    }
}
