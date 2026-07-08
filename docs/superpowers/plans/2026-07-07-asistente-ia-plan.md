# Asistente de IA (US21/US22/US23) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `ayni-asistente-ia-service`, an 8th microservice for Somos Ayni that answers questions about a reto (US21/SCRUM-61), gives learning recommendations (US22/SCRUM-62), and gives feedback on a solution approach (US23/SCRUM-63), using Spring AI against OpenRouter.

**Architecture:** Hexagonal/DDD microservice, same shape as the other 7 services (`domain/application/infrastructure`, JWT filter copied verbatim, shared Postgres). No tables of its own — reads `reto`, `postulacion`, `habilidad_validada` as read-only JPA "snapshots" (same pattern as `metricas-service`). Spring AI's OpenAI-compatible client points at OpenRouter's API.

**Tech Stack:** Java 21, Spring Boot 3.4.2 (this service only — the other 7 stay on 3.2.5; Spring AI 1.0.3 requires Boot ≥3.4), Spring AI 1.0.3 (`spring-ai-openai-spring-boot-starter`), Spring Data JPA, Spring Security (JWT, HS256), springdoc-openapi 2.6.0, PostgreSQL 16, Docker.

## Global Constraints

- Package root: `com.somosayni.asistente`. Shared code lives in `com.somosayni.shared` (copied verbatim from other services, not a library).
- Service port: `8088`.
- No `@ControllerAdvice` / global exception handler — matches every other service in this org. Unhandled exceptions fall through to Spring Boot's default error response.
- No new tables. `ddl-auto: none`. Every entity here is a read-only mapping onto another service's table.
- `OPENROUTER_API_KEY` is never written to a file that gets committed. Local dev uses a `.env` (gitignored). Production is a manual step in the Render dashboard (`sync: false` in `render.yaml`), exactly like `JWT_SECRET`.
- Default OpenRouter model: `deepseek/deepseek-chat-v3.1:free`, overridable via `OPENROUTER_MODEL` env var.
- Every request handler that fails validation (not found / not owned) throws `IllegalArgumentException` — same convention as `RetoController.obtenerReto()` in `retos-service`.
- Spec: `docs/superpowers/specs/2026-07-07-asistente-ia-design.md` (this repo).

---

## Task 1 — Scaffold + US21 "Consultar al asistente IA sobre un reto" (SCRUM-61)

**Files:**
- Create: `pom.xml`
- Create: `.gitignore`
- Create: `.env.example`
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-docker.yml`
- Create: `src/main/java/com/somosayni/asistente/AsistenteApplication.java`
- Create: `src/main/java/com/somosayni/shared/domain/model/AggregateRoot.java`
- Create: `src/main/java/com/somosayni/shared/domain/model/BaseEntity.java`
- Create: `src/main/java/com/somosayni/shared/domain/model/ValueObject.java`
- Create: `src/main/java/com/somosayni/shared/infrastructure/config/CorsConfig.java`
- Create: `src/main/java/com/somosayni/shared/infrastructure/config/OpenApiConfig.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/config/JwtService.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/config/JwtServiceImpl.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/config/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/config/SecurityConfig.java`
- Create: `src/main/java/com/somosayni/asistente/domain/model/RetoContexto.java`
- Create: `src/main/java/com/somosayni/asistente/application/port/RetoContextoRepository.java`
- Create: `src/main/java/com/somosayni/asistente/application/port/AsistenteIAPort.java`
- Create: `src/main/java/com/somosayni/asistente/application/query/ConsultarRetoQuery.java`
- Create: `src/main/java/com/somosayni/asistente/application/query/ConsultarRetoQueryHandler.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/RetoSnapshot.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaRetoSnapshotRepository.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/RetoContextoRepositoryImpl.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/ai/SpringAiAsistenteAdapter.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/rest/dto/ConsultaRetoRequest.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/rest/dto/ConsultaRetoResponse.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/rest/AsistenteController.java`
- Create: `README.md`
- Test: `src/test/java/com/somosayni/asistente/application/query/ConsultarRetoQueryHandlerTest.java`

**Interfaces:**
- Produces (used by Task 2 and Task 3): `AsistenteIAPort.responder(String systemPrompt, String userPrompt) -> String`; `RetoContextoRepository.obtenerPorId(String retoId) -> Optional<RetoContexto>`; `RetoContexto(String id, String titulo, String descripcion, String categoria, String nivelDificultad, List<String> requisitos)`.

- [ ] **Step 1: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/>
    </parent>

    <groupId>com.somosayni</groupId>
    <artifactId>ayni-asistente-ia-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>Ayni Asistente IA Service</name>

    <properties>
        <java.version>21</java.version>
        <spring-ai.version>1.0.3</spring-ai.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.6.0</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create `.gitignore` and `.env.example`**

`.gitignore`:
```
target/
*.class
*.jar
*.war
.env
.idea/
*.iml
.vscode/
*.DS_Store
```

`.env.example`:
```
JWT_SECRET=somosayni-jwt-secret-key-que-debe-ser-muy-larga-para-hs256-algoritmo-seguro
DB_USERNAME=somosayni
DB_PASSWORD=somosayni123
OPENROUTER_API_KEY=
OPENROUTER_MODEL=deepseek/deepseek-chat-v3.1:free
```

- [ ] **Step 3: Create the copied `shared` package**

`src/main/java/com/somosayni/shared/domain/model/AggregateRoot.java`:
```java
package com.somosayni.shared.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AggregateRoot {

    private String id;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateRoot that = (AggregateRoot) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public record DomainEvent(
            String eventType,
            String aggregateId,
            LocalDateTime occurredAt
    ) {}
}
```

`src/main/java/com/somosayni/shared/domain/model/BaseEntity.java`:
```java
package com.somosayni.shared.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

`src/main/java/com/somosayni/shared/domain/model/ValueObject.java`:
```java
package com.somosayni.shared.domain.model;

public abstract class ValueObject {
}
```

`src/main/java/com/somosayni/shared/infrastructure/config/CorsConfig.java`:
```java
package com.somosayni.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

`src/main/java/com/somosayni/shared/infrastructure/config/OpenApiConfig.java`:
```java
package com.somosayni.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ayni Asistente IA API")
                        .version("1.0.0")
                        .description("Microservicio de asistente de IA - Somos Ayni")
                        .contact(new Contact()
                                .name("Somos Ayni")
                                .url("https://somosayni.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

- [ ] **Step 4: Create the JWT config (copied from `retos-service`, package renamed)**

`src/main/java/com/somosayni/asistente/infrastructure/config/JwtService.java`:
```java
package com.somosayni.asistente.infrastructure.config;

public interface JwtService {
    String generateToken(String userId, String email, String rol);
    String getUserIdFromToken(String token);
    boolean validateToken(String token);
}
```

`src/main/java/com/somosayni/asistente/infrastructure/config/JwtServiceImpl.java`:
```java
package com.somosayni.asistente.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtServiceImpl implements JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(String userId, String email, String rol) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

`src/main/java/com/somosayni/asistente/infrastructure/config/JwtAuthenticationFilter.java`:
```java
package com.somosayni.asistente.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.validateToken(token)) {
                String userId = jwtService.getUserIdFromToken(token);

                HttpServletRequestWrapper securedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) return userId;
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return Collections.enumeration(Collections.singletonList(userId));
                        }
                        return super.getHeaders(name);
                    }

                    @Override
                    public Enumeration<String> getHeaderNames() {
                        List<String> names = Collections.list(super.getHeaderNames());
                        if (names.stream().noneMatch(n -> n.equalsIgnoreCase("X-User-Id"))) {
                            names.add("X-User-Id");
                        }
                        return Collections.enumeration(names);
                    }
                };

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(securedRequest, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

`src/main/java/com/somosayni/asistente/infrastructure/config/SecurityConfig.java`:
```java
package com.somosayni.asistente.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

- [ ] **Step 5: Create `AsistenteApplication.java`**

```java
package com.somosayni.asistente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.somosayni.asistente", "com.somosayni.shared"})
public class AsistenteApplication {
    public static void main(String[] args) {
        SpringApplication.run(AsistenteApplication.class, args);
    }
}
```

- [ ] **Step 6: Create `application.yml` and `application-docker.yml`**

`src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: ayni-asistente-ia-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:somosayni}
    username: ${DB_USERNAME:somosayni}
    password: ${DB_PASSWORD:somosayni123}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: ${OPENROUTER_MODEL:deepseek/deepseek-chat-v3.1:free}

server:
  port: ${PORT:8088}

jwt:
  secret: ${JWT_SECRET:somosayni-jwt-secret-key-que-debe-ser-muy-larga-para-hs256-algoritmo-seguro}
  expiration-ms: ${JWT_EXPIRATION:86400000}

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    com.somosayni.asistente: DEBUG
```

`src/main/resources/application-docker.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/somosayni
```

- [ ] **Step 7: Create `Dockerfile` and `docker-compose.yml`**

`Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
```

`docker-compose.yml`:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ayni-asistente-db
    environment:
      POSTGRES_DB: somosayni
      POSTGRES_USER: somosayni
      POSTGRES_PASSWORD: somosayni123
    ports:
      - "5439:5432"
    volumes:
      - asistente_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U somosayni"]
      interval: 10s
      timeout: 5s
      retries: 5

  asistente-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ayni-asistente-ia-service
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_USERNAME: somosayni
      DB_PASSWORD: somosayni123
      JWT_SECRET: ${JWT_SECRET}
      OPENROUTER_API_KEY: ${OPENROUTER_API_KEY}
      OPENROUTER_MODEL: ${OPENROUTER_MODEL:-deepseek/deepseek-chat-v3.1:free}
    ports:
      - "8088:8088"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  asistente_data:
```

- [ ] **Step 8: Verify the empty shell boots**

```bash
cd ayni-asistente-ia-service
export OPENROUTER_API_KEY=your-real-openrouter-key   # never commit this
export JWT_SECRET=somosayni-jwt-secret-key-que-debe-ser-muy-larga-para-hs256-algoritmo-seguro
mvn -q clean compile
```
Expected: `BUILD SUCCESS`, no errors (there's no `main` method conflict, no controller yet).

- [ ] **Step 9: Commit the scaffold**

```bash
git add pom.xml .gitignore .env.example Dockerfile docker-compose.yml src
git commit -m "$(cat <<'EOF'
chore: scaffold ayni-asistente-ia-service

Spring Boot 3.4.2 (bumped from the org's usual 3.2.5 — Spring AI 1.0.3
requires it), hexagonal layout, JWT/CORS/OpenAPI config copied from the
other services, Spring AI OpenAI-compatible client wired to OpenRouter.
EOF
)"
```

- [ ] **Step 10: Write the domain model and ports for US21**

`src/main/java/com/somosayni/asistente/domain/model/RetoContexto.java`:
```java
package com.somosayni.asistente.domain.model;

import java.util.List;

public record RetoContexto(
        String id,
        String titulo,
        String descripcion,
        String categoria,
        String nivelDificultad,
        List<String> requisitos
) {}
```

`src/main/java/com/somosayni/asistente/application/port/RetoContextoRepository.java`:
```java
package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.RetoContexto;

import java.util.Optional;

public interface RetoContextoRepository {
    Optional<RetoContexto> obtenerPorId(String retoId);
}
```

`src/main/java/com/somosayni/asistente/application/port/AsistenteIAPort.java`:
```java
package com.somosayni.asistente.application.port;

public interface AsistenteIAPort {
    String responder(String systemPrompt, String userPrompt);
}
```

- [ ] **Step 11: Write the failing test for `ConsultarRetoQueryHandler`**

`src/test/java/com/somosayni/asistente/application/query/ConsultarRetoQueryHandlerTest.java`:
```java
package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarRetoQueryHandlerTest {

    @Mock
    RetoContextoRepository retoContextoRepository;

    @Mock
    AsistenteIAPort asistenteIAPort;

    @Test
    void respondeConElTextoQueDevuelveElPuertoDeIaCuandoElRetoExiste() {
        RetoContexto reto = new RetoContexto("reto-1", "Landing page en React",
                "Construir una landing responsive", "FRONTEND", "JUNIOR",
                List.of("Usar React", "Ser responsive"));
        when(retoContextoRepository.obtenerPorId("reto-1")).thenReturn(Optional.of(reto));
        when(asistenteIAPort.responder(anyString(), anyString())).thenReturn("Respuesta de la IA");

        ConsultarRetoQueryHandler handler = new ConsultarRetoQueryHandler(retoContextoRepository, asistenteIAPort);
        String respuesta = handler.handle(new ConsultarRetoQuery("reto-1", "¿Qué tecnologías necesito?"));

        assertThat(respuesta).isEqualTo("Respuesta de la IA");
        verify(asistenteIAPort).responder(anyString(), anyString());
    }

    @Test
    void lanzaIllegalArgumentExceptionCuandoElRetoNoExiste() {
        when(retoContextoRepository.obtenerPorId("no-existe")).thenReturn(Optional.empty());

        ConsultarRetoQueryHandler handler = new ConsultarRetoQueryHandler(retoContextoRepository, asistenteIAPort);

        assertThatThrownBy(() -> handler.handle(new ConsultarRetoQuery("no-existe", "¿Qué necesito?")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-existe");
    }
}
```

- [ ] **Step 12: Run the test to verify it fails to compile (handler doesn't exist yet)**

```bash
mvn -q test -Dtest=ConsultarRetoQueryHandlerTest
```
Expected: compile error — `ConsultarRetoQuery` / `ConsultarRetoQueryHandler` cannot be resolved.

- [ ] **Step 13: Implement `ConsultarRetoQuery` and its handler**

`src/main/java/com/somosayni/asistente/application/query/ConsultarRetoQuery.java`:
```java
package com.somosayni.asistente.application.query;

public record ConsultarRetoQuery(String retoId, String pregunta) {}
```

`src/main/java/com/somosayni/asistente/application/query/ConsultarRetoQueryHandler.java`:
```java
package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.springframework.stereotype.Component;

@Component
public class ConsultarRetoQueryHandler {

    private final RetoContextoRepository retoContextoRepository;
    private final AsistenteIAPort asistenteIAPort;

    public ConsultarRetoQueryHandler(RetoContextoRepository retoContextoRepository, AsistenteIAPort asistenteIAPort) {
        this.retoContextoRepository = retoContextoRepository;
        this.asistenteIAPort = asistenteIAPort;
    }

    public String handle(ConsultarRetoQuery query) {
        RetoContexto reto = retoContextoRepository.obtenerPorId(query.retoId())
                .orElseThrow(() -> new IllegalArgumentException("Reto no encontrado: " + query.retoId()));

        String systemPrompt = """
                Eres el asistente de IA de Somos Ayni, una plataforma de empleabilidad juvenil peruana.
                Ayudas a talentos a entender un reto práctico antes de postular.
                Responde en español, de forma clara y concisa, basándote solo en el contexto del reto dado.
                """;

        String userPrompt = """
                Reto: %s
                Descripción: %s
                Categoría: %s
                Nivel de dificultad: %s
                Requisitos: %s

                Pregunta del talento: %s
                """.formatted(
                reto.titulo(),
                reto.descripcion(),
                reto.categoria(),
                reto.nivelDificultad(),
                String.join(", ", reto.requisitos()),
                query.pregunta());

        return asistenteIAPort.responder(systemPrompt, userPrompt);
    }
}
```

- [ ] **Step 14: Run the test again to verify it passes**

```bash
mvn -q test -Dtest=ConsultarRetoQueryHandlerTest
```
Expected: `BUILD SUCCESS`, 2 tests passed.

- [ ] **Step 15: Commit**

```bash
git add src/main/java/com/somosayni/asistente/domain src/main/java/com/somosayni/asistente/application src/test
git commit -m "feat: add ConsultarRetoQueryHandler with mocked ports (US21)"
```

- [ ] **Step 16: Implement the `RetoSnapshot` read-only entity and its Spring Data repository**

`src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/RetoSnapshot.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reto")
public class RetoSnapshot {

    @Id
    private String id;

    private String titulo;

    @Column(length = 4000)
    private String descripcion;

    private String categoria;

    @Column(name = "nivel_dificultad")
    private String nivelDificultad;

    @ElementCollection
    @CollectionTable(name = "reto_requisito", joinColumns = @JoinColumn(name = "reto_id"))
    @Column(name = "descripcion")
    private List<String> requisitos = new ArrayList<>();

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public String getNivelDificultad() { return nivelDificultad; }
    public List<String> getRequisitos() { return requisitos; }
}
```

`src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaRetoSnapshotRepository.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.RetoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRetoSnapshotRepository extends JpaRepository<RetoSnapshot, String> {
}
```

`src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/RetoContextoRepositoryImpl.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.mapper;

import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.RetoContexto;
import com.somosayni.asistente.infrastructure.persistence.entity.RetoSnapshot;
import com.somosayni.asistente.infrastructure.persistence.repository.JpaRetoSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RetoContextoRepositoryImpl implements RetoContextoRepository {

    private final JpaRetoSnapshotRepository jpaRepository;

    public RetoContextoRepositoryImpl(JpaRetoSnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RetoContexto> obtenerPorId(String retoId) {
        return jpaRepository.findById(retoId).map(this::toDomain);
    }

    private RetoContexto toDomain(RetoSnapshot snapshot) {
        List<String> requisitos = snapshot.getRequisitos() == null ? List.of() : snapshot.getRequisitos();
        return new RetoContexto(
                snapshot.getId(),
                snapshot.getTitulo(),
                snapshot.getDescripcion(),
                snapshot.getCategoria(),
                snapshot.getNivelDificultad(),
                requisitos);
    }
}
```

- [ ] **Step 17: Implement the Spring AI adapter**

`src/main/java/com/somosayni/asistente/infrastructure/ai/SpringAiAsistenteAdapter.java`:
```java
package com.somosayni.asistente.infrastructure.ai;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class SpringAiAsistenteAdapter implements AsistenteIAPort {

    private final ChatClient chatClient;

    public SpringAiAsistenteAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String responder(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
```

- [ ] **Step 18: Implement the REST layer for US21**

`src/main/java/com/somosayni/asistente/infrastructure/rest/dto/ConsultaRetoRequest.java`:
```java
package com.somosayni.asistente.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultaRetoRequest(@NotBlank(message = "La pregunta es obligatoria") String pregunta) {}
```

`src/main/java/com/somosayni/asistente/infrastructure/rest/dto/ConsultaRetoResponse.java`:
```java
package com.somosayni.asistente.infrastructure.rest.dto;

public record ConsultaRetoResponse(String respuesta) {}
```

`src/main/java/com/somosayni/asistente/infrastructure/rest/AsistenteController.java`:
```java
package com.somosayni.asistente.infrastructure.rest;

import com.somosayni.asistente.application.query.ConsultarRetoQuery;
import com.somosayni.asistente.application.query.ConsultarRetoQueryHandler;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoRequest;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final ConsultarRetoQueryHandler consultarRetoHandler;

    public AsistenteController(ConsultarRetoQueryHandler consultarRetoHandler) {
        this.consultarRetoHandler = consultarRetoHandler;
    }

    @PostMapping("/retos/{retoId}/consulta")
    public ResponseEntity<ConsultaRetoResponse> consultarSobreReto(
            @PathVariable String retoId,
            @Valid @RequestBody ConsultaRetoRequest request) {
        String respuesta = consultarRetoHandler.handle(new ConsultarRetoQuery(retoId, request.pregunta()));
        return ResponseEntity.ok(new ConsultaRetoResponse(respuesta));
    }
}
```

- [ ] **Step 19: Write `README.md`**

```markdown
# 🤖 Ayni Asistente IA Service

Microservicio de IA de la plataforma **Somos Ayni**, construido con **Spring AI**
contra un modelo gratuito de [OpenRouter](https://openrouter.ai). No tiene tablas
propias: lee en modo solo-lectura las tablas de `retos-service`, `postulaciones-service`
y `habilidades-service` (mismo patrón que `metricas-service`).

## Responsabilidad del Bounded Context

**Maneja:**
- US21 (SCRUM-61): responder preguntas de un talento sobre un reto específico.
- US22 (SCRUM-62): recomendaciones de aprendizaje personalizadas.
- US23 (SCRUM-63): feedback sobre el enfoque de solución antes de entregar.

**NO maneja:**
- Creación ni edición de retos, postulaciones o habilidades (eso es de sus
  respectivos servicios).
- Persistencia de conversaciones: cada request es independiente (stateless).

## Endpoints REST

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/asistente/retos/{retoId}/consulta` | Preguntar sobre un reto (US21) | JWT |

### Body de `POST /api/v1/asistente/retos/{retoId}/consulta`

```json
{ "pregunta": "¿Qué tecnologías necesito para este reto?" }
```

Respuesta:
```json
{ "respuesta": "Para este reto necesitas..." }
```

## Arquitectura (Hexagonal)

```
src/main/java/com/somosayni/asistente/
├── domain/model/          # RetoContexto, PostulacionContexto, HabilidadContexto, RecomendacionAprendizaje
├── application/
│   ├── port/               # RetoContextoRepository, AsistenteIAPort, ...
│   └── query/               # ConsultarRetoQuery(Handler), ...
└── infrastructure/
    ├── ai/                  # SpringAiAsistenteAdapter (Spring AI -> OpenRouter)
    ├── persistence/          # entidades *Snapshot de solo lectura + adapters
    ├── rest/                 # AsistenteController + DTOs
    └── config/               # JWT, Security, CORS, OpenAPI
```

## Cómo ejecutar

### Local

```bash
# Requisitos: Java 21, Maven 3.9+, PostgreSQL 16 con el esquema `somosayni`
# ya poblado por retos-service/postulaciones-service/habilidades-service
# (este servicio no crea tablas: ddl-auto=none)

export OPENROUTER_API_KEY=tu-api-key-de-openrouter
mvn clean package -DskipTests
java -jar target/*.jar
```

### Docker

```bash
cp .env.example .env
# Editar .env con tus valores reales (JWT_SECRET, OPENROUTER_API_KEY)
docker-compose up --build
```

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Conexión a la Postgres compartida `somosayni` | `localhost` / `5432` / `somosayni` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de la BD | `somosayni` / `somosayni123` |
| `JWT_SECRET` | Clave compartida para validar JWT firmados por `identidad-service` | *(obligatorio, mismo valor en los 8 servicios)* |
| `OPENROUTER_API_KEY` | API key de OpenRouter | *(obligatorio, nunca se commitea)* |
| `OPENROUTER_MODEL` | Modelo de OpenRouter a usar | `deepseek/deepseek-chat-v3.1:free` |
| `PORT` | Puerto del servicio | `8088` |

## Swagger / OpenAPI

| | Link |
|---|---|
| **Swagger UI (local)** | [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html) |

> Para probar los endpoints: copia el JWT del login de `identidad-service` → clic en **Authorize** → pega `Bearer <tu-token>`.
```

- [ ] **Step 20: Manual end-to-end verification (real OpenRouter call)**

```bash
# Terminal 1: shared Postgres (skip if already running)
docker run -d --name ayni-postgres -e POSTGRES_DB=somosayni \
  -e POSTGRES_USER=somosayni -e POSTGRES_PASSWORD=somosayni123 \
  -p 5432:5432 postgres:16-alpine

# Terminal 2: identidad-service (creates the `usuario` table + issues JWTs)
cd ../ayni-identidad-service && mvn spring-boot:run

# Terminal 3: retos-service (creates the `reto`/`reto_requisito` tables this service reads)
cd ../ayni-retos-service && mvn spring-boot:run

# Terminal 4: this service
cd ayni-asistente-ia-service
export OPENROUTER_API_KEY=tu-api-key-de-openrouter
mvn spring-boot:run
```

```bash
# Register an EMPRESA and get a JWT
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"email":"empresa-demo@ayni.com","password":"password123","rol":"EMPRESA"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Publish a reto
RETO_ID=$(curl -s -X POST http://localhost:8083/api/v1/retos \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"titulo":"Landing page en React","descripcion":"Construir una landing responsive con formulario de contacto","categoria":"FRONTEND","requisitos":["Usar React","Ser responsive"],"entregables":["Repositorio con el código"],"tipoRecompensa":"DIPLOMA","montoRecompensa":0,"fechaLimite":"2026-12-31","nivelDificultad":"JUNIOR","cuposDisponibles":5}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
curl -s -X POST http://localhost:8083/api/v1/retos/$RETO_ID/cerrar -H "Authorization: Bearer $TOKEN" >/dev/null || true

# Ask the assistant about it
curl -s -X POST http://localhost:8088/api/v1/asistente/retos/$RETO_ID/consulta \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"pregunta":"¿Qué tecnologías necesito para resolver este reto?"}'
```
Expected: `200 OK` with `{"respuesta": "..."}` containing a real answer generated by the OpenRouter model. Take this screenshot/terminal output for your presentation.

- [ ] **Step 21: Commit persistence/AI/REST layer + README**

```bash
git add src/main/java/com/somosayni/asistente/infrastructure README.md
git commit -m "feat: US21 - consultar al asistente IA sobre un reto (SCRUM-61)"
```

- [ ] **Step 22: Push the branch and open the PR**

```bash
git checkout -b feature/scrum-61-consulta-reto
git push -u origin feature/scrum-61-consulta-reto
gh pr create --repo ayni-01/ayni-asistente-ia-service \
  --title "US21 (SCRUM-61): Consultar al asistente IA sobre un reto" \
  --body "$(cat <<'EOF'
## Resumen
- Scaffold del microservicio `ayni-asistente-ia-service` (hexagonal, JWT compartido, sin tablas propias).
- Integración Spring AI 1.0.3 contra OpenRouter (modelo configurable vía `OPENROUTER_MODEL`).
- `POST /api/v1/asistente/retos/{retoId}/consulta`: el talento pregunta sobre un reto y recibe respuesta generada con el contexto real del reto (lectura solo-lectura de la tabla `reto`).

## Historia
SCRUM-61 / US21 — Consultar al asistente IA sobre un reto.

## Test plan
- [x] `ConsultarRetoQueryHandlerTest` (feliz + reto no encontrado)
- [x] Verificación manual end-to-end contra OpenRouter (ver README)
EOF
)"
```

- [ ] **Step 23: Merge the PR**

```bash
gh pr merge --repo ayni-01/ayni-asistente-ia-service --merge --delete-branch
git checkout main
git pull
```

---

## Task 2 — US22 "Recibir recomendaciones de aprendizaje" (SCRUM-62)

**Files:**
- Create: `src/main/java/com/somosayni/asistente/domain/model/HabilidadContexto.java`
- Create: `src/main/java/com/somosayni/asistente/domain/model/RecomendacionAprendizaje.java`
- Create: `src/main/java/com/somosayni/asistente/application/port/HabilidadContextoRepository.java`
- Modify: `src/main/java/com/somosayni/asistente/application/port/RetoContextoRepository.java` (add `obtenerActivos()`)
- Modify: `src/main/java/com/somosayni/asistente/application/port/AsistenteIAPort.java` (add `responderRecomendaciones(...)`)
- Create: `src/main/java/com/somosayni/asistente/application/query/ObtenerRecomendacionesQuery.java`
- Create: `src/main/java/com/somosayni/asistente/application/query/ObtenerRecomendacionesQueryHandler.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/HabilidadValidadaSnapshot.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaHabilidadValidadaSnapshotRepository.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/HabilidadContextoRepositoryImpl.java`
- Modify: `src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/RetoSnapshot.java` (add `estado` field)
- Modify: `src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaRetoSnapshotRepository.java` (add `findByEstado`)
- Modify: `src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/RetoContextoRepositoryImpl.java` (implement `obtenerActivos()`)
- Modify: `src/main/java/com/somosayni/asistente/infrastructure/ai/SpringAiAsistenteAdapter.java` (implement `responderRecomendaciones(...)`)
- Create: `src/main/java/com/somosayni/asistente/infrastructure/rest/dto/RecomendacionesResponse.java`
- Modify: `src/main/java/com/somosayni/asistente/infrastructure/rest/AsistenteController.java` (add `GET /recomendaciones`)
- Modify: `README.md` (add US22 endpoint)
- Test: `src/test/java/com/somosayni/asistente/application/query/ObtenerRecomendacionesQueryHandlerTest.java`

**Interfaces:**
- Consumes (from Task 1): `RetoContextoRepository`, `AsistenteIAPort.responder(...)`, `RetoContexto`.
- Produces (used by Task 3 — Task 3 does NOT need these, but keep the port shape stable): `RecomendacionAprendizaje(String tema, String motivo, String nivelSugerido)`; `AsistenteIAPort.responderRecomendaciones(String systemPrompt, String userPrompt) -> List<RecomendacionAprendizaje>`; `HabilidadContextoRepository.obtenerPorTalentoId(String talentoId) -> List<HabilidadContexto>`; `RetoContextoRepository.obtenerActivos() -> List<RetoContexto>`.

- [ ] **Step 1: Start the branch**

```bash
git checkout -b feature/scrum-62-recomendaciones
```

- [ ] **Step 2: Add the new domain records**

`src/main/java/com/somosayni/asistente/domain/model/HabilidadContexto.java`:
```java
package com.somosayni.asistente.domain.model;

public record HabilidadContexto(String nombre, String nivel, int porcentaje) {}
```

`src/main/java/com/somosayni/asistente/domain/model/RecomendacionAprendizaje.java`:
```java
package com.somosayni.asistente.domain.model;

public record RecomendacionAprendizaje(String tema, String motivo, String nivelSugerido) {}
```

- [ ] **Step 3: Extend the ports**

`src/main/java/com/somosayni/asistente/application/port/HabilidadContextoRepository.java` (new file):
```java
package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.HabilidadContexto;

import java.util.List;

public interface HabilidadContextoRepository {
    List<HabilidadContexto> obtenerPorTalentoId(String talentoId);
}
```

Modify `src/main/java/com/somosayni/asistente/application/port/RetoContextoRepository.java` — full new content:
```java
package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.RetoContexto;

import java.util.List;
import java.util.Optional;

public interface RetoContextoRepository {
    Optional<RetoContexto> obtenerPorId(String retoId);
    List<RetoContexto> obtenerActivos();
}
```

Modify `src/main/java/com/somosayni/asistente/application/port/AsistenteIAPort.java` — full new content:
```java
package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;

import java.util.List;

public interface AsistenteIAPort {
    String responder(String systemPrompt, String userPrompt);
    List<RecomendacionAprendizaje> responderRecomendaciones(String systemPrompt, String userPrompt);
}
```

- [ ] **Step 4: Run the build to confirm the now-broken implementations show up**

```bash
mvn -q compile
```
Expected: compile errors in `RetoContextoRepositoryImpl` and `SpringAiAsistenteAdapter` — they no longer satisfy their interfaces. This is expected; fixed in the next steps.

- [ ] **Step 5: Write the failing test for `ObtenerRecomendacionesQueryHandler`**

`src/test/java/com/somosayni/asistente/application/query/ObtenerRecomendacionesQueryHandlerTest.java`:
```java
package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerRecomendacionesQueryHandlerTest {

    @Mock
    HabilidadContextoRepository habilidadContextoRepository;

    @Mock
    RetoContextoRepository retoContextoRepository;

    @Mock
    AsistenteIAPort asistenteIAPort;

    @Test
    void incluyeLasHabilidadesDelTalentoEnElPromptCuandoTiene() {
        when(habilidadContextoRepository.obtenerPorTalentoId("talento-1"))
                .thenReturn(List.of(new HabilidadContexto("React", "INTERMEDIO", 60)));
        when(retoContextoRepository.obtenerActivos())
                .thenReturn(List.of(new RetoContexto("reto-1", "t", "d", "BACKEND", "JUNIOR", List.of())));
        List<RecomendacionAprendizaje> esperado = List.of(
                new RecomendacionAprendizaje("Spring Boot", "Hay retos activos de BACKEND", "JUNIOR"));
        when(asistenteIAPort.responderRecomendaciones(anyString(), anyString())).thenReturn(esperado);

        ObtenerRecomendacionesQueryHandler handler = new ObtenerRecomendacionesQueryHandler(
                habilidadContextoRepository, retoContextoRepository, asistenteIAPort);
        List<RecomendacionAprendizaje> resultado = handler.handle(new ObtenerRecomendacionesQuery("talento-1"));

        assertThat(resultado).isEqualTo(esperado);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(asistenteIAPort).responderRecomendaciones(anyString(), userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue()).contains("React").contains("BACKEND");
    }

    @Test
    void usaUnPromptGenericoCuandoElTalentoNoTieneHabilidadesValidadas() {
        when(habilidadContextoRepository.obtenerPorTalentoId("talento-2")).thenReturn(List.of());
        when(retoContextoRepository.obtenerActivos()).thenReturn(List.of());
        when(asistenteIAPort.responderRecomendaciones(anyString(), anyString())).thenReturn(List.of());

        ObtenerRecomendacionesQueryHandler handler = new ObtenerRecomendacionesQueryHandler(
                habilidadContextoRepository, retoContextoRepository, asistenteIAPort);
        handler.handle(new ObtenerRecomendacionesQuery("talento-2"));

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(asistenteIAPort).responderRecomendaciones(anyString(), userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue()).contains("aún no tiene habilidades validadas");
    }
}
```

- [ ] **Step 6: Run the test to verify it fails (handler doesn't exist yet)**

```bash
mvn -q test -Dtest=ObtenerRecomendacionesQueryHandlerTest
```
Expected: compile error — `ObtenerRecomendacionesQuery`/`Handler` not found.

- [ ] **Step 7: Implement `ObtenerRecomendacionesQuery` and its handler**

`src/main/java/com/somosayni/asistente/application/query/ObtenerRecomendacionesQuery.java`:
```java
package com.somosayni.asistente.application.query;

public record ObtenerRecomendacionesQuery(String talentoId) {}
```

`src/main/java/com/somosayni/asistente/application/query/ObtenerRecomendacionesQueryHandler.java`:
```java
package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ObtenerRecomendacionesQueryHandler {

    private final HabilidadContextoRepository habilidadContextoRepository;
    private final RetoContextoRepository retoContextoRepository;
    private final AsistenteIAPort asistenteIAPort;

    public ObtenerRecomendacionesQueryHandler(
            HabilidadContextoRepository habilidadContextoRepository,
            RetoContextoRepository retoContextoRepository,
            AsistenteIAPort asistenteIAPort) {
        this.habilidadContextoRepository = habilidadContextoRepository;
        this.retoContextoRepository = retoContextoRepository;
        this.asistenteIAPort = asistenteIAPort;
    }

    public List<RecomendacionAprendizaje> handle(ObtenerRecomendacionesQuery query) {
        List<HabilidadContexto> habilidades = habilidadContextoRepository.obtenerPorTalentoId(query.talentoId());
        List<RetoContexto> retosActivos = retoContextoRepository.obtenerActivos();

        String habilidadesTexto = habilidades.isEmpty()
                ? "El talento aún no tiene habilidades validadas."
                : habilidades.stream()
                        .map(h -> "%s (nivel %s, %d%%)".formatted(h.nombre(), h.nivel(), h.porcentaje()))
                        .collect(Collectors.joining(", "));

        String categoriasDemandadas = retosActivos.stream()
                .map(RetoContexto::categoria)
                .distinct()
                .collect(Collectors.joining(", "));

        String systemPrompt = """
                Eres el asistente de IA de Somos Ayni, una plataforma de empleabilidad juvenil peruana.
                Generas recomendaciones de aprendizaje personalizadas para talentos jóvenes.
                Responde siempre con 3 a 5 recomendaciones concretas, en español.
                """;

        String userPrompt = """
                Habilidades validadas del talento: %s
                Categorías con retos activos en la plataforma: %s

                Recomienda temas de aprendizaje que le ayuden a cerrar la brecha entre lo que sabe
                y lo que la plataforma está demandando.
                """.formatted(habilidadesTexto, categoriasDemandadas);

        return asistenteIAPort.responderRecomendaciones(systemPrompt, userPrompt);
    }
}
```

- [ ] **Step 8: Run the test again to verify it passes**

```bash
mvn -q test -Dtest=ObtenerRecomendacionesQueryHandlerTest
```
Expected: `BUILD SUCCESS`, 2 tests passed.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/somosayni/asistente/domain src/main/java/com/somosayni/asistente/application src/test
git commit -m "feat: add ObtenerRecomendacionesQueryHandler with mocked ports (US22)"
```

- [ ] **Step 10: Implement the `HabilidadValidadaSnapshot` entity and repository**

`src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/HabilidadValidadaSnapshot.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "habilidad_validada")
public class HabilidadValidadaSnapshot {

    @Id
    private String id;

    @Column(name = "talento_id")
    private String talentoId;

    private String nombre;

    private String nivel;

    private int porcentaje;

    public String getId() { return id; }
    public String getTalentoId() { return talentoId; }
    public String getNombre() { return nombre; }
    public String getNivel() { return nivel; }
    public int getPorcentaje() { return porcentaje; }
}
```

`src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaHabilidadValidadaSnapshotRepository.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.HabilidadValidadaSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaHabilidadValidadaSnapshotRepository extends JpaRepository<HabilidadValidadaSnapshot, String> {
    List<HabilidadValidadaSnapshot> findByTalentoId(String talentoId);
}
```

`src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/HabilidadContextoRepositoryImpl.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.mapper;

import com.somosayni.asistente.application.port.HabilidadContextoRepository;
import com.somosayni.asistente.domain.model.HabilidadContexto;
import com.somosayni.asistente.infrastructure.persistence.repository.JpaHabilidadValidadaSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HabilidadContextoRepositoryImpl implements HabilidadContextoRepository {

    private final JpaHabilidadValidadaSnapshotRepository jpaRepository;

    public HabilidadContextoRepositoryImpl(JpaHabilidadValidadaSnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<HabilidadContexto> obtenerPorTalentoId(String talentoId) {
        return jpaRepository.findByTalentoId(talentoId).stream()
                .map(s -> new HabilidadContexto(s.getNombre(), s.getNivel(), s.getPorcentaje()))
                .toList();
    }
}
```

- [ ] **Step 11: Add `estado` to `RetoSnapshot` and finish `RetoContextoRepositoryImpl`**

Modify `src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/RetoSnapshot.java` — full new content:
```java
package com.somosayni.asistente.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reto")
public class RetoSnapshot {

    @Id
    private String id;

    private String titulo;

    @Column(length = 4000)
    private String descripcion;

    private String categoria;

    @Column(name = "nivel_dificultad")
    private String nivelDificultad;

    private String estado;

    @ElementCollection
    @CollectionTable(name = "reto_requisito", joinColumns = @JoinColumn(name = "reto_id"))
    @Column(name = "descripcion")
    private List<String> requisitos = new ArrayList<>();

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public String getNivelDificultad() { return nivelDificultad; }
    public String getEstado() { return estado; }
    public List<String> getRequisitos() { return requisitos; }
}
```

Modify `src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaRetoSnapshotRepository.java` — full new content:
```java
package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.RetoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRetoSnapshotRepository extends JpaRepository<RetoSnapshot, String> {
    List<RetoSnapshot> findByEstado(String estado);
}
```

Modify `src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/RetoContextoRepositoryImpl.java` — full new content:
```java
package com.somosayni.asistente.infrastructure.persistence.mapper;

import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.RetoContexto;
import com.somosayni.asistente.infrastructure.persistence.entity.RetoSnapshot;
import com.somosayni.asistente.infrastructure.persistence.repository.JpaRetoSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RetoContextoRepositoryImpl implements RetoContextoRepository {

    private final JpaRetoSnapshotRepository jpaRepository;

    public RetoContextoRepositoryImpl(JpaRetoSnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RetoContexto> obtenerPorId(String retoId) {
        return jpaRepository.findById(retoId).map(this::toDomain);
    }

    @Override
    public List<RetoContexto> obtenerActivos() {
        return jpaRepository.findByEstado("ACTIVO").stream().map(this::toDomain).toList();
    }

    private RetoContexto toDomain(RetoSnapshot snapshot) {
        List<String> requisitos = snapshot.getRequisitos() == null ? List.of() : snapshot.getRequisitos();
        return new RetoContexto(
                snapshot.getId(),
                snapshot.getTitulo(),
                snapshot.getDescripcion(),
                snapshot.getCategoria(),
                snapshot.getNivelDificultad(),
                requisitos);
    }
}
```

- [ ] **Step 12: Finish `SpringAiAsistenteAdapter`**

Modify `src/main/java/com/somosayni/asistente/infrastructure/ai/SpringAiAsistenteAdapter.java` — full new content:
```java
package com.somosayni.asistente.infrastructure.ai;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpringAiAsistenteAdapter implements AsistenteIAPort {

    private final ChatClient chatClient;

    public SpringAiAsistenteAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String responder(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public List<RecomendacionAprendizaje> responderRecomendaciones(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(new ParameterizedTypeReference<List<RecomendacionAprendizaje>>() {});
    }
}
```

- [ ] **Step 13: Run the full build to confirm everything compiles and tests pass**

```bash
mvn -q clean test
```
Expected: `BUILD SUCCESS`, all 4 tests passed (2 from Task 1, 2 from this task).

- [ ] **Step 14: Add the REST endpoint**

`src/main/java/com/somosayni/asistente/infrastructure/rest/dto/RecomendacionesResponse.java`:
```java
package com.somosayni.asistente.infrastructure.rest.dto;

import com.somosayni.asistente.domain.model.RecomendacionAprendizaje;

import java.util.List;

public record RecomendacionesResponse(List<RecomendacionAprendizaje> recomendaciones) {}
```

Modify `src/main/java/com/somosayni/asistente/infrastructure/rest/AsistenteController.java` — full new content:
```java
package com.somosayni.asistente.infrastructure.rest;

import com.somosayni.asistente.application.query.ConsultarRetoQuery;
import com.somosayni.asistente.application.query.ConsultarRetoQueryHandler;
import com.somosayni.asistente.application.query.ObtenerRecomendacionesQuery;
import com.somosayni.asistente.application.query.ObtenerRecomendacionesQueryHandler;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoRequest;
import com.somosayni.asistente.infrastructure.rest.dto.ConsultaRetoResponse;
import com.somosayni.asistente.infrastructure.rest.dto.RecomendacionesResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final ConsultarRetoQueryHandler consultarRetoHandler;
    private final ObtenerRecomendacionesQueryHandler recomendacionesHandler;

    public AsistenteController(
            ConsultarRetoQueryHandler consultarRetoHandler,
            ObtenerRecomendacionesQueryHandler recomendacionesHandler) {
        this.consultarRetoHandler = consultarRetoHandler;
        this.recomendacionesHandler = recomendacionesHandler;
    }

    @PostMapping("/retos/{retoId}/consulta")
    public ResponseEntity<ConsultaRetoResponse> consultarSobreReto(
            @PathVariable String retoId,
            @Valid @RequestBody ConsultaRetoRequest request) {
        String respuesta = consultarRetoHandler.handle(new ConsultarRetoQuery(retoId, request.pregunta()));
        return ResponseEntity.ok(new ConsultaRetoResponse(respuesta));
    }

    @GetMapping("/recomendaciones")
    public ResponseEntity<RecomendacionesResponse> obtenerRecomendaciones(
            @RequestHeader("X-User-Id") String talentoId) {
        var recomendaciones = recomendacionesHandler.handle(new ObtenerRecomendacionesQuery(talentoId));
        return ResponseEntity.ok(new RecomendacionesResponse(recomendaciones));
    }
}
```

- [ ] **Step 15: Update `README.md`**

Add this row to the endpoints table:
```markdown
| `GET` | `/api/v1/asistente/recomendaciones` | Recomendaciones de aprendizaje personalizadas (US22) | JWT |
```

- [ ] **Step 16: Manual verification (needs a talento with a validated skill — habilidades-service)**

```bash
# Terminal 5
cd ../ayni-habilidades-service && mvn spring-boot:run
```
```bash
TALENTO_TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"email":"talento-demo@ayni.com","password":"password123","rol":"TALENTO"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s http://localhost:8088/api/v1/asistente/recomendaciones \
  -H "Authorization: Bearer $TALENTO_TOKEN"
```
Expected: `200 OK` with `{"recomendaciones": [...]}` — since this talento has no validated skills yet, the model should still return generic entry-level recommendations (that's the branch covered by the second unit test).

- [ ] **Step 17: Commit and open the PR**

```bash
git add src/main/java/com/somosayni/asistente README.md
git commit -m "feat: US22 - recibir recomendaciones de aprendizaje (SCRUM-62)"
git push -u origin feature/scrum-62-recomendaciones
gh pr create --repo ayni-01/ayni-asistente-ia-service \
  --title "US22 (SCRUM-62): Recibir recomendaciones de aprendizaje" \
  --body "$(cat <<'EOF'
## Resumen
- `GET /api/v1/asistente/recomendaciones`: lee las habilidades validadas del talento (`habilidad_validada`) y las categorías de retos activos, y le pide al modelo 3-5 recomendaciones de aprendizaje con salida estructurada (Spring AI `entity(ParameterizedTypeReference<List<...>>)`).
- Extiende `AsistenteIAPort` y `RetoContextoRepository` (añadidos en la PR de US21) sin romper su forma existente.

## Historia
SCRUM-62 / US22 — Recibir recomendaciones de aprendizaje.

## Test plan
- [x] `ObtenerRecomendacionesQueryHandlerTest` (con habilidades + sin habilidades)
- [x] Verificación manual end-to-end contra OpenRouter
EOF
)"
```

- [ ] **Step 18: Merge**

```bash
gh pr merge --repo ayni-01/ayni-asistente-ia-service --merge --delete-branch
git checkout main
git pull
```

---

## Task 3 — US23 "Obtener retroalimentación sobre el enfoque de solución" (SCRUM-63)

**Files:**
- Create: `src/main/java/com/somosayni/asistente/domain/model/PostulacionContexto.java`
- Create: `src/main/java/com/somosayni/asistente/application/port/PostulacionContextoRepository.java`
- Create: `src/main/java/com/somosayni/asistente/application/query/ObtenerFeedbackSolucionQuery.java`
- Create: `src/main/java/com/somosayni/asistente/application/query/ObtenerFeedbackSolucionQueryHandler.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/PostulacionSnapshot.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaPostulacionSnapshotRepository.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/PostulacionContextoRepositoryImpl.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/rest/dto/FeedbackSolucionRequest.java`
- Create: `src/main/java/com/somosayni/asistente/infrastructure/rest/dto/FeedbackSolucionResponse.java`
- Modify: `src/main/java/com/somosayni/asistente/infrastructure/rest/AsistenteController.java` (add `POST /postulaciones/{id}/feedback`)
- Modify: `README.md` (add US23 endpoint)
- Test: `src/test/java/com/somosayni/asistente/application/query/ObtenerFeedbackSolucionQueryHandlerTest.java`

**Interfaces:**
- Consumes (from Task 1, unchanged): `AsistenteIAPort.responder(String, String) -> String`, `RetoContextoRepository.obtenerPorId(String) -> Optional<RetoContexto>`.
- Produces: `PostulacionContexto(String id, String talentoId, String retoId)`; `PostulacionContextoRepository.obtenerPorId(String) -> Optional<PostulacionContexto>`.

- [ ] **Step 1: Start the branch**

```bash
git checkout -b feature/scrum-63-feedback-solucion
```

- [ ] **Step 2: Add the domain record and port**

`src/main/java/com/somosayni/asistente/domain/model/PostulacionContexto.java`:
```java
package com.somosayni.asistente.domain.model;

public record PostulacionContexto(String id, String talentoId, String retoId) {}
```

`src/main/java/com/somosayni/asistente/application/port/PostulacionContextoRepository.java`:
```java
package com.somosayni.asistente.application.port;

import com.somosayni.asistente.domain.model.PostulacionContexto;

import java.util.Optional;

public interface PostulacionContextoRepository {
    Optional<PostulacionContexto> obtenerPorId(String postulacionId);
}
```

- [ ] **Step 3: Write the failing test for `ObtenerFeedbackSolucionQueryHandler`**

`src/test/java/com/somosayni/asistente/application/query/ObtenerFeedbackSolucionQueryHandlerTest.java`:
```java
package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.PostulacionContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.PostulacionContexto;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerFeedbackSolucionQueryHandlerTest {

    @Mock
    PostulacionContextoRepository postulacionContextoRepository;

    @Mock
    RetoContextoRepository retoContextoRepository;

    @Mock
    AsistenteIAPort asistenteIAPort;

    private ObtenerFeedbackSolucionQueryHandler handler() {
        return new ObtenerFeedbackSolucionQueryHandler(
                postulacionContextoRepository, retoContextoRepository, asistenteIAPort);
    }

    @Test
    void devuelveFeedbackCuandoLaPostulacionPerteneceAlTalento() {
        when(postulacionContextoRepository.obtenerPorId("post-1"))
                .thenReturn(Optional.of(new PostulacionContexto("post-1", "talento-1", "reto-1")));
        when(retoContextoRepository.obtenerPorId("reto-1"))
                .thenReturn(Optional.of(new RetoContexto("reto-1", "t", "d", "BACKEND", "JUNIOR", List.of())));
        when(asistenteIAPort.responder(anyString(), anyString())).thenReturn("Buen enfoque, pero...");

        String feedback = handler().handle(
                new ObtenerFeedbackSolucionQuery("post-1", "talento-1", "Voy a usar arquitectura MVC"));

        assertThat(feedback).isEqualTo("Buen enfoque, pero...");
    }

    @Test
    void lanzaExcepcionSiLaPostulacionNoExiste() {
        when(postulacionContextoRepository.obtenerPorId("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler().handle(
                new ObtenerFeedbackSolucionQuery("no-existe", "talento-1", "enfoque")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lanzaExcepcionSiLaPostulacionNoPerteneceAlTalentoAutenticado() {
        when(postulacionContextoRepository.obtenerPorId("post-1"))
                .thenReturn(Optional.of(new PostulacionContexto("post-1", "otro-talento", "reto-1")));

        assertThatThrownBy(() -> handler().handle(
                new ObtenerFeedbackSolucionQuery("post-1", "talento-1", "enfoque")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 4: Run the test to verify it fails to compile**

```bash
mvn -q test -Dtest=ObtenerFeedbackSolucionQueryHandlerTest
```
Expected: compile error — `ObtenerFeedbackSolucionQuery`/`Handler` not found.

- [ ] **Step 5: Implement `ObtenerFeedbackSolucionQuery` and its handler**

`src/main/java/com/somosayni/asistente/application/query/ObtenerFeedbackSolucionQuery.java`:
```java
package com.somosayni.asistente.application.query;

public record ObtenerFeedbackSolucionQuery(String postulacionId, String talentoId, String enfoqueSolucion) {}
```

`src/main/java/com/somosayni/asistente/application/query/ObtenerFeedbackSolucionQueryHandler.java`:
```java
package com.somosayni.asistente.application.query;

import com.somosayni.asistente.application.port.AsistenteIAPort;
import com.somosayni.asistente.application.port.PostulacionContextoRepository;
import com.somosayni.asistente.application.port.RetoContextoRepository;
import com.somosayni.asistente.domain.model.PostulacionContexto;
import com.somosayni.asistente.domain.model.RetoContexto;
import org.springframework.stereotype.Component;

@Component
public class ObtenerFeedbackSolucionQueryHandler {

    private final PostulacionContextoRepository postulacionContextoRepository;
    private final RetoContextoRepository retoContextoRepository;
    private final AsistenteIAPort asistenteIAPort;

    public ObtenerFeedbackSolucionQueryHandler(
            PostulacionContextoRepository postulacionContextoRepository,
            RetoContextoRepository retoContextoRepository,
            AsistenteIAPort asistenteIAPort) {
        this.postulacionContextoRepository = postulacionContextoRepository;
        this.retoContextoRepository = retoContextoRepository;
        this.asistenteIAPort = asistenteIAPort;
    }

    public String handle(ObtenerFeedbackSolucionQuery query) {
        PostulacionContexto postulacion = postulacionContextoRepository.obtenerPorId(query.postulacionId())
                .orElseThrow(() -> new IllegalArgumentException("Postulación no encontrada: " + query.postulacionId()));

        if (!postulacion.talentoId().equals(query.talentoId())) {
            throw new IllegalArgumentException("La postulación no pertenece al talento autenticado");
        }

        RetoContexto reto = retoContextoRepository.obtenerPorId(postulacion.retoId())
                .orElseThrow(() -> new IllegalArgumentException("Reto no encontrado: " + postulacion.retoId()));

        String systemPrompt = """
                Eres el asistente de IA de Somos Ayni, una plataforma de empleabilidad juvenil peruana.
                Das retroalimentación constructiva sobre el enfoque de solución que un talento propone
                para un reto, antes de que lo entregue. Responde en español: menciona fortalezas,
                riesgos y sugerencias concretas.
                """;

        String userPrompt = """
                Reto: %s
                Descripción: %s
                Requisitos: %s

                Enfoque de solución propuesto por el talento: %s
                """.formatted(
                reto.titulo(),
                reto.descripcion(),
                String.join(", ", reto.requisitos()),
                query.enfoqueSolucion());

        return asistenteIAPort.responder(systemPrompt, userPrompt);
    }
}
```

- [ ] **Step 6: Run the test again to verify it passes**

```bash
mvn -q test -Dtest=ObtenerFeedbackSolucionQueryHandlerTest
```
Expected: `BUILD SUCCESS`, 3 tests passed.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/somosayni/asistente/domain src/main/java/com/somosayni/asistente/application src/test
git commit -m "feat: add ObtenerFeedbackSolucionQueryHandler with mocked ports (US23)"
```

- [ ] **Step 8: Implement the `PostulacionSnapshot` entity and repository**

`src/main/java/com/somosayni/asistente/infrastructure/persistence/entity/PostulacionSnapshot.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "postulacion")
public class PostulacionSnapshot {

    @Id
    private String id;

    @Column(name = "talento_id")
    private String talentoId;

    @Column(name = "reto_id")
    private String retoId;

    public String getId() { return id; }
    public String getTalentoId() { return talentoId; }
    public String getRetoId() { return retoId; }
}
```

`src/main/java/com/somosayni/asistente/infrastructure/persistence/repository/JpaPostulacionSnapshotRepository.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.PostulacionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPostulacionSnapshotRepository extends JpaRepository<PostulacionSnapshot, String> {
}
```

`src/main/java/com/somosayni/asistente/infrastructure/persistence/mapper/PostulacionContextoRepositoryImpl.java`:
```java
package com.somosayni.asistente.infrastructure.persistence.mapper;

import com.somosayni.asistente.application.port.PostulacionContextoRepository;
import com.somosayni.asistente.domain.model.PostulacionContexto;
import com.somosayni.asistente.infrastructure.persistence.repository.JpaPostulacionSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PostulacionContextoRepositoryImpl implements PostulacionContextoRepository {

    private final JpaPostulacionSnapshotRepository jpaRepository;

    public PostulacionContextoRepositoryImpl(JpaPostulacionSnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PostulacionContexto> obtenerPorId(String postulacionId) {
        return jpaRepository.findById(postulacionId)
                .map(s -> new PostulacionContexto(s.getId(), s.getTalentoId(), s.getRetoId()));
    }
}
```

- [ ] **Step 9: Run the full build**

```bash
mvn -q clean test
```
Expected: `BUILD SUCCESS`, all 7 tests passed (2 + 2 + 3).

- [ ] **Step 10: Add the REST endpoint**

`src/main/java/com/somosayni/asistente/infrastructure/rest/dto/FeedbackSolucionRequest.java`:
```java
package com.somosayni.asistente.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedbackSolucionRequest(
        @NotBlank(message = "El enfoque de solución es obligatorio") String enfoqueSolucion
) {}
```

`src/main/java/com/somosayni/asistente/infrastructure/rest/dto/FeedbackSolucionResponse.java`:
```java
package com.somosayni.asistente.infrastructure.rest.dto;

public record FeedbackSolucionResponse(String feedback) {}
```

Modify `src/main/java/com/somosayni/asistente/infrastructure/rest/AsistenteController.java` — full new content:
```java
package com.somosayni.asistente.infrastructure.rest;

import com.somosayni.asistente.application.query.*;
import com.somosayni.asistente.infrastructure.rest.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/asistente")
public class AsistenteController {

    private final ConsultarRetoQueryHandler consultarRetoHandler;
    private final ObtenerRecomendacionesQueryHandler recomendacionesHandler;
    private final ObtenerFeedbackSolucionQueryHandler feedbackHandler;

    public AsistenteController(
            ConsultarRetoQueryHandler consultarRetoHandler,
            ObtenerRecomendacionesQueryHandler recomendacionesHandler,
            ObtenerFeedbackSolucionQueryHandler feedbackHandler) {
        this.consultarRetoHandler = consultarRetoHandler;
        this.recomendacionesHandler = recomendacionesHandler;
        this.feedbackHandler = feedbackHandler;
    }

    @PostMapping("/retos/{retoId}/consulta")
    public ResponseEntity<ConsultaRetoResponse> consultarSobreReto(
            @PathVariable String retoId,
            @Valid @RequestBody ConsultaRetoRequest request) {
        String respuesta = consultarRetoHandler.handle(new ConsultarRetoQuery(retoId, request.pregunta()));
        return ResponseEntity.ok(new ConsultaRetoResponse(respuesta));
    }

    @GetMapping("/recomendaciones")
    public ResponseEntity<RecomendacionesResponse> obtenerRecomendaciones(
            @RequestHeader("X-User-Id") String talentoId) {
        var recomendaciones = recomendacionesHandler.handle(new ObtenerRecomendacionesQuery(talentoId));
        return ResponseEntity.ok(new RecomendacionesResponse(recomendaciones));
    }

    @PostMapping("/postulaciones/{postulacionId}/feedback")
    public ResponseEntity<FeedbackSolucionResponse> obtenerFeedback(
            @PathVariable String postulacionId,
            @RequestHeader("X-User-Id") String talentoId,
            @Valid @RequestBody FeedbackSolucionRequest request) {
        String feedback = feedbackHandler.handle(
                new ObtenerFeedbackSolucionQuery(postulacionId, talentoId, request.enfoqueSolucion()));
        return ResponseEntity.ok(new FeedbackSolucionResponse(feedback));
    }
}
```

- [ ] **Step 11: Update `README.md`**

Add this row to the endpoints table:
```markdown
| `POST` | `/api/v1/asistente/postulaciones/{postulacionId}/feedback` | Feedback sobre el enfoque de solución (US23) | JWT (dueño) |
```

- [ ] **Step 12: Manual verification**

```bash
# Terminal 6: postulaciones-service (creates the `postulacion` table)
cd ../ayni-postulaciones-service && mvn spring-boot:run
```
```bash
POST_ID=$(curl -s -X POST http://localhost:8084/api/v1/postulaciones \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TALENTO_TOKEN" \
  -d "{\"retoId\":\"$RETO_ID\",\"urlSolucion\":\"https://github.com/demo/solucion\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

curl -s -X POST http://localhost:8088/api/v1/asistente/postulaciones/$POST_ID/feedback \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TALENTO_TOKEN" \
  -d '{"enfoqueSolucion":"Voy a usar componentes funcionales de React con hooks y CSS Grid para el layout responsive"}'
```
Expected: `200 OK` with `{"feedback": "..."}` — real feedback generated by OpenRouter based on the actual reto + the described approach.

- [ ] **Step 13: Commit and open the PR**

```bash
git add src/main/java/com/somosayni/asistente README.md
git commit -m "feat: US23 - obtener retroalimentacion sobre el enfoque de solucion (SCRUM-63)"
git push -u origin feature/scrum-63-feedback-solucion
gh pr create --repo ayni-01/ayni-asistente-ia-service \
  --title "US23 (SCRUM-63): Obtener retroalimentación sobre el enfoque de solución" \
  --body "$(cat <<'EOF'
## Resumen
- `POST /api/v1/asistente/postulaciones/{postulacionId}/feedback`: el talento describe su enfoque de solución en texto libre y recibe feedback generado con el contexto real del reto y de su propia postulación.
- Valida que la postulación exista y pertenezca al talento autenticado (`X-User-Id` inyectado por el filtro JWT) antes de llamar al modelo.
- Reutiliza `AsistenteIAPort.responder(...)` de la PR de US21 sin modificarlo.

## Historia
SCRUM-63 / US23 — Obtener retroalimentación sobre el enfoque de solución.

## Test plan
- [x] `ObtenerFeedbackSolucionQueryHandlerTest` (feliz, postulación no encontrada, postulación de otro talento)
- [x] Verificación manual end-to-end contra OpenRouter
EOF
)"
```

- [ ] **Step 14: Merge**

```bash
gh pr merge --repo ayni-01/ayni-asistente-ia-service --merge --delete-branch
git checkout main
git pull
```

---

## Task 4 — Update org documentation (`.github` and `ayni-infra`)

**Files:**
- Modify: `../ayni-infra/render.yaml`
- Modify: `../ayni-infra/README.md`
- Modify: `../.github/profile/README.md`

**Interfaces:** None (documentation only).

- [ ] **Step 1: Add the 8th service to `render.yaml`**

Modify `../ayni-infra/render.yaml` — append after the `metricas-service` block (after the closing of its `envVars` list, i.e. after line 255 `sync: false`):
```yaml

  # ─────────────────────────────────────────
  # 8. asistente-ia-service  (puerto 8088)
  #    Lee snapshots de reto/postulacion/habilidad_validada — desplegar
  #    después de retos, postulaciones y habilidades.
  # ─────────────────────────────────────────
  - type: web
    name: ayni-asistente-ia-service
    runtime: docker
    repo: https://github.com/ayni-01/ayni-asistente-ia-service
    branch: main
    plan: free
    region: oregon
    healthCheckPath: /actuator/health
    envVars:
      - key: DB_HOST
        fromDatabase:
          name: ayni-postgres
          property: host
      - key: DB_PORT
        fromDatabase:
          name: ayni-postgres
          property: port
      - key: DB_USERNAME
        fromDatabase:
          name: ayni-postgres
          property: user
      - key: DB_PASSWORD
        fromDatabase:
          name: ayni-postgres
          property: password
      - key: DB_NAME
        fromDatabase:
          name: ayni-postgres
          property: database
      - key: JWT_SECRET
        sync: false
      - key: OPENROUTER_API_KEY
        sync: false
      - key: OPENROUTER_MODEL
        value: deepseek/deepseek-chat-v3.1:free
```

- [ ] **Step 2: Update `../ayni-infra/README.md`**

In the "Paso obligatorio post-despliegue" section, change point 2's instructions to mention both variables (replace the existing `JWT_SECRET = ...` code block with):
```
   JWT_SECRET = somosayni-jwt-secret-key-que-debe-ser-muy-larga-para-hs256

   > El valor debe ser **idéntico** en los 8 servicios. Si difiere, los tokens de `identidad-service` serán rechazados por los demás.

   Además, solo en `ayni-asistente-ia-service`:
   ```
   OPENROUTER_API_KEY = <tu api key de openrouter.ai>
   ```
```

In the "Recursos creados" table, add a row:
```markdown
| `ayni-asistente-ia-service` | Web Service (Docker) | free |
```

In "URLs de producción", add a row:
```markdown
| asistente-ia | `https://ayni-asistente-ia-service.onrender.com` |
```

- [ ] **Step 3: Update `../.github/profile/README.md`**

In the "Repositorios" table, add a row after `metricas-service`:
```markdown
| **asistente-ia-service** | [ayni-01/ayni-asistente-ia-service](https://github.com/ayni-01/ayni-asistente-ia-service) | 8088 | Asistente de IA: consultas sobre retos, recomendaciones de aprendizaje y feedback de soluciones (Spring AI + OpenRouter) |
```

In the Mermaid architecture diagram, add the node and its edges (inside the `org` subgraph, after `MS`):
```
        AIS["asistente-ia-service\n:8088\n/api/v1/asistente"]
```
and add:
```
    FE -->|"Bearer JWT"| AIS
    AIS -->|"lectura snapshot\ntablas: reto, postulacion, habilidad_validada"| DB
```

In the "Estrategia de Base de Datos" table, add a row:
```markdown
| asistente-ia | _(sin tablas propias — solo lectura snapshot)_ | `none` |
```

In "Herramientas de desarrollo", add a row:
```markdown
| **Swagger UI asistente-ia** | http://localhost:8088/swagger-ui.html |
```

In the "Levantar Todo Localmente" section, add to the recommended startup order (after `metricas`):
```markdown
# 5. asistente-ia al final (lee tablas de retos, postulaciones y habilidades)
cd ayni-asistente-ia-service && mvn spring-boot:run &
```

- [ ] **Step 4: Commit and push directly to `main` in both repos**

```bash
cd ../ayni-infra
git add render.yaml README.md
git commit -m "docs: add ayni-asistente-ia-service (8th microservice)"
git push origin main

cd ../.github
git add profile/README.md
git commit -m "docs: add ayni-asistente-ia-service to the org architecture overview"
git push origin main
```

- [ ] **Step 5: Verify both pushes landed**

```bash
gh repo view ayni-01/ayni-infra --json defaultBranchRef -q .defaultBranchRef.name
git -C ../ayni-infra log --oneline -1
git -C ../.github log --oneline -1
```
Expected: both show the new commit as the tip of `main`.

---

## Final check

- [ ] All 3 PRs merged in `ayni-01/ayni-asistente-ia-service` — screenshot these for the presentation (`gh pr list --repo ayni-01/ayni-asistente-ia-service --state merged`).
- [ ] `mvn -q clean test` passes on `main` (7 tests: 2+2+3).
- [ ] `.github` and `ayni-infra` `main` branches show the documentation commits.
- [ ] Reminder: `OPENROUTER_API_KEY` still needs to be pasted manually into the Render dashboard for `ayni-asistente-ia-service` before the first production deploy — it is not, and must never be, in any commit.
