package com.esprit.microservice.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = SecurityRoutesTest.SecurityProbeController.class)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        SecurityRoutesTest.SecurityProbeController.class
})
class SecurityRoutesTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void publicCanReadEvents() {
        webTestClient.get()
                .uri("/api/events")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void eventNotificationAggregationRequiresOrganizerRole() {
        webTestClient.get()
                .uri("/api/events/with-notification")
                .exchange()
                .expectStatus().isUnauthorized();

        clientWithRole("USER").get()
                .uri("/api/events/with-notification")
                .exchange()
                .expectStatus().isForbidden();

        clientWithRole("ORGANIZER").get()
                .uri("/api/events/with-notification")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void anonymousUserCannotRegister() {
        webTestClient.post()
                .uri("/api/events/event-1/registrations")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void assignedUserRoleCanRegisterAndCancelRegistration() {
        WebTestClient authenticatedClient = clientWithRole("USER");

        authenticatedClient.post()
                .uri("/api/events/event-1/registrations")
                .exchange()
                .expectStatus().isOk();
        authenticatedClient.delete()
                .uri("/api/events/event-1/registrations/user-1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userCannotManageEventsButOrganizerCan() {
        clientWithRole("USER").post()
                .uri("/api/events")
                .exchange()
                .expectStatus().isForbidden();

        clientWithRole("ORGANIZER").post()
                .uri("/api/events")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userCanReadAndAcknowledgeNotifications() {
        WebTestClient userClient = clientWithRole("USER");

        userClient.get()
                .uri("/api/notifications")
                .exchange()
                .expectStatus().isOk();
        userClient.patch()
                .uri("/api/notifications/notification-1/read")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userCannotCreateNotificationButOrganizerCan() {
        clientWithRole("USER").post()
                .uri("/api/notifications")
                .exchange()
                .expectStatus().isForbidden();

        clientWithRole("ORGANIZER").post()
                .uri("/api/notifications")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void publicCanReadProductsButOnlyAdminCanManageThem() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/api/products")
                .exchange()
                .expectStatus().isUnauthorized();

        clientWithRole("USER").post()
                .uri("/api/products")
                .exchange()
                .expectStatus().isForbidden();

        clientWithRole("ADMIN").post()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void authenticatedTokenWithoutApplicationRoleIsForbidden() {
        webTestClient.mutateWith(mockJwt()).get()
                .uri("/api/notifications")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
    }

    private WebTestClient clientWithRole(String role) {
        return webTestClient.mutateWith(mockJwt().authorities(
                new SimpleGrantedAuthority("ROLE_" + role)
        ));
    }

    @RestController
    static class SecurityProbeController {

        @GetMapping("/api/events")
        String getEvents() {
            return "events";
        }

        @GetMapping("/api/events/with-notification")
        String getEventsWithNotifications() {
            return "events-with-notifications";
        }

        @PostMapping("/api/events")
        String createEvent() {
            return "created";
        }

        @PostMapping("/api/events/{eventId}/registrations")
        String register() {
            return "registered";
        }

        @DeleteMapping("/api/events/{eventId}/registrations/{participantId}")
        String cancelRegistration() {
            return "cancelled";
        }

        @GetMapping("/api/notifications")
        String getNotifications() {
            return "notifications";
        }

        @PostMapping("/api/notifications")
        String createNotification() {
            return "created";
        }

        @PatchMapping("/api/notifications/{notificationId}/read")
        String markNotificationRead() {
            return "read";
        }

        @GetMapping("/api/products")
        String getProducts() {
            return "products";
        }

        @PostMapping("/api/products")
        String createProduct() {
            return "created";
        }
    }
}
