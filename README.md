# CampConnect API Gateway

Reactive Spring Cloud Gateway for the CampConnect microservice platform.
It provides service discovery routing, centralized Keycloak security, CORS,
health monitoring, and an aggregated Swagger UI.

## Routes

| External route | Target | Access |
| --- | --- | --- |
| Normal `GET /api/events/**` catalogue routes | `event-service` | Public |
| Event/notification aggregation routes | `event-service` | `ADMIN` or `ORGANIZER` |
| Event registration endpoints | `event-service` | `USER`, `ADMIN`, or `ORGANIZER` |
| Event management endpoints | `event-service` | `ADMIN` or `ORGANIZER` |
| Notification reads/read-state | `notification-service` | Any assigned application role |
| Notification create/update/delete | `notification-service` | `ADMIN` or `ORGANIZER` |
| `/api/users/**` | `user-service` | Authenticated |
| `/api/site-camping/**` | `api-camping` | Authenticated |
| `/api/inscriptionsite/**` | `api-camping` | Authenticated |

The `/api` prefix is removed before event and notification requests are
forwarded because those services expose `/events` and `/notifications`.

## Keycloak

Realm roles are converted to Spring authorities:

- Keycloak `ADMIN` becomes `ROLE_ADMIN`.
- Keycloak `ORGANIZER` becomes `ROLE_ORGANIZER`.
- Keycloak `USER` becomes `ROLE_USER`.

Authentication without one of these application roles is not sufficient for
event registration or notification access. This keeps authorization decisions
centralized at the Gateway rather than duplicated across each microservice.

`KEYCLOAK_ISSUER_URI` is the browser-visible issuer in the token.
`KEYCLOAK_JWK_SET_URI` can use the Docker-internal Keycloak hostname. Keeping
these separate allows local browser login and container-side signature
verification to work together.

## OpenAPI

Open http://localhost:9001/swagger-ui.html and select:

- Events
- Notifications
- Camping

The gateway proxies each service's `/v3/api-docs` document through `/openapi/*`.

## Configuration

| Variable | Default |
| --- | --- |
| `EUREKA_URL` | `http://eureka:8761/eureka/` |
| `KEYCLOAK_ISSUER_URI` | `http://keycloak:8080/realms/campconnect` |
| `KEYCLOAK_JWK_SET_URI` | Keycloak internal certificate endpoint |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` |
| `GATEWAY_LOG_LEVEL` | `INFO` |

## Build and Test

```bash
./mvnw test
docker build -t campconnect/api-gateway .
```

Health is available at `/actuator/health`.
