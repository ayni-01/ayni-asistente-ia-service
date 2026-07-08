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
