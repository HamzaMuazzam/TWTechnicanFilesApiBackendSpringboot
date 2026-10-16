package com.trackingworld.tw.common.config;

import com.trackingworld.tw.common.ApiPaths;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    public static final String BEARER = "Bearer Authentication";
    public static final String API_KEY = "Ingest API Key";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).bearerFormat("JWT").scheme("bearer"))
                        .addSecuritySchemes(API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Api-Key")))
                .info(new Info().title("TrackingWorld Notification API")
                        .description("""
                                TrackingWorld Notification backend: legacy technician file uploads, \
                                S3 alert ingestion, the customer Android app API and the admin dashboard API.""")
                        .version("1.0").contact(new Contact().name("Hamza Muazzam")
                                .email("hamzamuazzam@gmail.com")));
    }

    @Bean
    public GroupedOpenApi filesApi() {
        return GroupedOpenApi.builder().group("4 - files (technician)").pathsToMatch(ApiPaths.TECHNICIAN_BASE + "/**").build();
    }

    @Bean
    public GroupedOpenApi ingestApi() {
        return GroupedOpenApi.builder().group("1 - ingest (S3 → us)").pathsToMatch(ApiPaths.INGEST + "/**").build();
    }

    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder().group("2 - app (Android)").pathsToMatch(ApiPaths.APP + "/**").build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("3 - admin (dashboard)").pathsToMatch(ApiPaths.ADMIN + "/**").build();
    }

}
