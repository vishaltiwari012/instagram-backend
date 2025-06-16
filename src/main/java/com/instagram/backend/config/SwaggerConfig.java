package com.instagram.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Instagram Clone - Monolithic Backend API")
                        .description("This API provides endpoints for authentication, user management, posts, stories, comments, likes, and followers management to mimic Instagram functionality.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Vishal Tiwari")
                                .email("vishal@example.com")
                                .url("https://github.com/vishaltiwari012"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org"))
                )
                .tags(List.of(
                        new Tag().name("Auth APIs").description("Authentication and User Registration APIs"),
                        new Tag().name("User APIs").description("User profile, followers, and relationship management APIs"),
                        new Tag().name("Post APIs").description("Post creation, interaction (likes/comments), retrieval, and deletion APIs"),
                        new Tag().name("Notification APIs").description("APIs to manage user notifications, alerts, and messaging"),
                        new Tag().name("Admin APIs").description("Administrative APIs for managing users, content, and system settings")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)))
                .servers(List.of(new Server().url("http://localhost:8081").description("Local Auth Server")));
    }
}
