package com.esprit.microservice.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator getRoute(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("api-camping-site", r -> r.path("/api/site-camping/**")
                        .uri("lb://api-camping"))

                .route("api-camping-inscription", r -> r.path("/api/inscriptionsite/**")
                        .uri("lb://api-camping"))

                .route("ap-users", r -> r.path("/api/users/**")
                        .uri("lb://ap-users"))

                .build();
    }

}
