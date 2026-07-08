# Asistente de IA — Diseño

Fecha: 2026-07-07
Historias: US21 (SCRUM-61), US22 (SCRUM-62), US23 (SCRUM-63)

## Contexto

Somos Ayni tiene 7 microservicios (hexagonal + DDD, Postgres compartida, JWT
HS256 firmado por `identidad-service`). Ninguno se comunica con otro vía
REST; `metricas-service` es el precedente para leer datos de otro bounded
context: entidades JPA de solo lectura (`*Snapshot`) sobre las mismas tablas.

Este documento diseña un octavo microservicio, `ayni-asistente-ia-service`,
que cubre 3 historias de usuario usando Spring AI:

- **US21** — Consultar al asistente IA sobre un reto.
- **US22** — Recibir recomendaciones de aprendizaje.
- **US23** — Obtener retroalimentación sobre el enfoque de solución.

## Decisiones

- **Servicio nuevo**, no repartido en los existentes: aísla la dependencia
  del proveedor de IA y las 3 historias comparten una sola responsabilidad
  ("asistir con IA"), lo cual es un bounded context legítimo.
- **Sin tablas propias.** El servicio no persiste conversaciones, preguntas
  ni feedback — cada request es independiente. Solo lee (read-only) de
  tablas ajenas vía snapshots, igual que `metricas-service`.
- **Proveedor**: OpenRouter (gateway OpenAI-compatible) vía
  `spring-ai-openai-spring-boot-starter` con `base-url` sobreescrito.
  Modelo configurable por env var, default `deepseek/deepseek-chat-v3.1:free`.
- **US23 no descarga la URL de la solución.** El talento describe su enfoque
  en texto libre en el request; no hay infraestructura para fetch seguro de
  URLs arbitrarias y sería innecesario para el caso de uso.
- **Manejo de errores**: igual que el resto del proyecto — excepciones sin
  `@ControllerAdvice` global (no existe en ningún otro servicio).

## Arquitectura

```
ayni-asistente-ia-service/
  src/main/java/com/somosayni/asistente/
    AsistenteApplication.java
    domain/model/
      RecomendacionAprendizaje.java        (record: tema, motivo, nivelSugerido)
    application/
      port/
        RetoContextoRepository.java         (obtener RetoContexto por id)
        PostulacionContextoRepository.java  (obtener PostulacionContexto por id)
        HabilidadContextoRepository.java    (listar habilidades por talentoId)
        AsistenteIAPort.java                (responder texto / responder estructurado)
      query/
        ConsultarRetoQuery.java + Handler          (US21)
        ObtenerRecomendacionesQuery.java + Handler (US22)
        ObtenerFeedbackSolucionQuery.java + Handler (US23)
    infrastructure/
      config/        JwtAuthenticationFilter, JwtService(Impl), SecurityConfig  (copiados 1:1 de otro servicio)
      persistence/
        entity/       RetoSnapshot, PostulacionSnapshot, HabilidadValidadaSnapshot
        repository/    JpaRetoSnapshotRepository, JpaPostulacionSnapshotRepository, JpaHabilidadValidadaSnapshotRepository
        mapper/        adapters que implementan los ports de application/port usando los Jpa*Repository
      ai/
        SpringAiAsistenteAdapter.java  (implementa AsistenteIAPort con ChatClient de Spring AI)
      rest/
        AsistenteController.java
        dto/           ConsultaRetoRequest/Response, RecomendacionesResponse, FeedbackSolucionRequest/Response
  src/main/java/com/somosayni/shared/   (copiado tal cual de los otros servicios)
  src/main/resources/application.yml, application-docker.yml
  Dockerfile, docker-compose.yml, pom.xml, README.md, postman_collection.json
```

Puerto: **8088**.

## Endpoints

| Historia | Método y ruta | Auth | Request | Response |
|---|---|---|---|---|
| US21 | `POST /api/v1/asistente/retos/{retoId}/consulta` | `X-User-Id` desde JWT | `{ "pregunta": string }` | `{ "respuesta": string }` |
| US22 | `GET /api/v1/asistente/recomendaciones` | `X-User-Id` desde JWT (talento) | — | `{ "recomendaciones": [{ "tema", "motivo", "nivelSugerido" }] }` |
| US23 | `POST /api/v1/asistente/postulaciones/{postulacionId}/feedback` | `X-User-Id` desde JWT (dueño) | `{ "enfoqueSolucion": string }` | `{ "feedback": string }` |

### US21 — Consultar sobre un reto

1. `ConsultarRetoQueryHandler` carga `RetoContexto` (título, descripción,
   categoría, requisitos, nivel) por `retoId`. Si no existe →
   `IllegalArgumentException` ("Reto no encontrado").
2. Arma un prompt: system message fija el rol ("eres un asistente que ayuda
   a talentos a entender un reto de la plataforma Somos Ayni") + contexto del
   reto serializado + la pregunta del usuario.
3. Llama `AsistenteIAPort.responder(prompt)` → texto libre.

### US22 — Recomendaciones de aprendizaje

1. `ObtenerRecomendacionesQueryHandler` lee todas las
   `HabilidadValidadaSnapshot` del `talentoId` (header `X-User-Id`) y las
   categorías/niveles de los retos `ACTIVO` disponibles.
2. Prompt pide 3-5 recomendaciones de aprendizaje en base a las brechas entre
   habilidades actuales y lo que piden los retos activos.
3. Usa `AsistenteIAPort.responderEstructurado(prompt, List<RecomendacionAprendizaje>.class)`
   — Spring AI `BeanOutputConverter` parsea el JSON del modelo directo a la
   lista de records, sin parsing manual.
4. Si el talento no tiene habilidades validadas, igual se generan
   recomendaciones genéricas de entrada (el prompt lo contempla).

### US23 — Feedback sobre el enfoque de solución

1. `ObtenerFeedbackSolucionQueryHandler` carga `PostulacionContexto` por
   `postulacionId`. Si no existe, o si `postulacionContexto.talentoId() !=
   X-User-Id`, lanza `IllegalArgumentException` — sin `@ControllerAdvice`,
   esto cae al manejador de errores por defecto de Spring Boot (500), igual
   que `RetoController.obtenerReto()` en `retos-service` cuando el reto no
   existe. Es el mismo nivel de manejo de errores que ya tiene el resto del
   proyecto, no se introduce nada nuevo aquí.
2. Carga el `RetoContexto` del `retoId` asociado para dar contexto de qué
   pedía el reto.
3. Prompt: contexto del reto (requisitos/entregables) + `enfoqueSolucion`
   del talento → pide feedback constructivo (fortalezas, riesgos, sugerencias).
4. Devuelve el texto de `AsistenteIAPort.responder(prompt)`.

## Spring AI / OpenRouter

`pom.xml` agrega `spring-ai-openai-spring-boot-starter` (BOM
`spring-ai-bom` en `<dependencyManagement>`, versión estable compatible con
Spring Boot 3.2.5).

`application.yml`:

```yaml
spring:
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: ${OPENROUTER_MODEL:deepseek/deepseek-chat-v3.1:free}
```

`OPENROUTER_API_KEY` es obligatorio (sin default) — sigue el mismo patrón que
`JWT_SECRET` en `render.yaml` (`sync: false`, se configura a mano en el
dashboard). Localmente va en un `.env` gitignored (el `.gitignore` copiado de
los otros servicios ya excluye `.env`).

## Seguridad

- Mismo `JwtAuthenticationFilter`/`JwtService`/`SecurityConfig` que los otros
  7 servicios, copiado tal cual (así es como el proyecto maneja código
  compartido — no hay librería común).
- El servicio nunca ve `JWT_SECRET` real vs API key: son env vars separadas.
- La API key de OpenRouter no se commitea en ningún archivo del repo.

## Testing

- Un test unitario por `QueryHandler` con el `AsistenteIAPort` y los
  `*ContextoRepository` mockeados (Mockito, ya usado implícitamente por
  `spring-boot-starter-test`), verificando: prompt incluye los datos
  esperados, manejo de "no encontrado", y mapeo de la respuesta del puerto al
  DTO de salida.
- No se agregan tests de integración contra OpenRouter real (llamaría a un
  servicio externo pago/rate-limited en CI) — se mockea `AsistenteIAPort`.

## Despliegue y documentación

- `ayni-infra/render.yaml`: agrega el bloque del 8vo servicio
  (`ayni-asistente-ia-service`, puerto 8088, mismas env vars de DB, más
  `OPENROUTER_API_KEY`/`OPENROUTER_MODEL` con `sync: false`).
- `ayni-infra/README.md`: agrega el paso manual de `OPENROUTER_API_KEY` a la
  tabla de "Paso obligatorio post-despliegue".
- `.github/profile/README.md`: agrega fila a la tabla de repositorios, nodo
  al diagrama Mermaid, sección de puerto/Swagger, entrada en variables de
  entorno.
- Estos 3 cambios van directo a `main` en sus repos (no son las historias a
  mostrar en las capturas de PR).

## Plan de PRs (para las capturas)

Repo `ayni-01/ayni-asistente-ia-service`, 3 PRs secuenciales (cada uno
mergeado antes de abrir el siguiente):

1. `feature/scrum-61-consulta-reto` → SCRUM-61 (US21): scaffold completo del
   servicio (pom, Dockerfile, shared/, JWT config, `RetoSnapshot` +
   `RetoContextoRepository`, Spring AI config, `AsistenteIAPort` +
   adapter) + endpoint de consulta sobre reto.
2. `feature/scrum-62-recomendaciones` → SCRUM-62 (US22):
   `HabilidadValidadaSnapshot` + `HabilidadContextoRepository` + endpoint de
   recomendaciones con salida estructurada.
3. `feature/scrum-63-feedback-solucion` → SCRUM-63 (US23):
   `PostulacionSnapshot` + `PostulacionContextoRepository` + endpoint de
   feedback de enfoque de solución.
