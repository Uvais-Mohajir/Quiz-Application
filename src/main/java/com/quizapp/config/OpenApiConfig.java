package com.quizapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI quizAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuizApp API")
                        .description("API documentation for QuizApp authentication, admin, mentor, and participant workflows")
                        .version("v1.0.0")
                        .contact(new Contact().name("QuizApp Team"))
                        .license(new License().name("Proprietary")));
    }
}
