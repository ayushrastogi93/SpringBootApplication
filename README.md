# Spring Boot Task Management API

This repository contains a Spring Boot 3.2.2 REST API for managing `Task` resources with JPA, H2 in-memory persistence, Spring Cache, asynchronous bulk updates, Resilience4j rate limiting, Actuator health/metrics exposure, and structured JSON error responses.

## Overview

The application exposes a small task-management API over HTTP. The domain model is a `Task` with a database-generated primary key and basic properties:

- `id` — numeric identifier
- `title` — task title
- `description` — task narrative
- `completed` — boolean progress flag

The service layer coordinates between the REST layer and a Spring Data JPA repository backed by H2.

## Technology Stack

- Java 17
- Spring Boot 3.2.2
- Spring Web
- Spring Data JPA
- Spring Cache
- Spring AOP
- Spring Actuator
- H2 in-memory database
- Resilience4j rate limiter
- Maven

## Project Structure

- [src/main/java/com/example/demo/DemoApplication.java](src/main/java/com/example/demo/DemoApplication.java) — Spring Boot bootstrap class
- [src/main/java/com/example/demo/controller/TaskController.java](src/main/java/com/example/demo/controller/TaskController.java) — HTTP API contract
- [src/main/java/com/example/demo/service/TaskService.java](src/main/java/com/example/demo/service/TaskService.java) — business logic, cache annotations, async bulk update
- [src/main/java/com/example/demo/repository/TaskRepository.java](src/main/java/com/example/demo/repository/TaskRepository.java) — JPA repository interface
- [src/main/java/com/example/demo/model/Task.java](src/main/java/com/example/demo/model/Task.java) — JPA entity
- [src/main/java/com/example/demo/exception/GlobalExceptionHandler.java](src/main/java/com/example/demo/exception/GlobalExceptionHandler.java) — central exception mapping
- [src/main/resources/application.yaml](src/main/resources/application.yaml) — externalized application configuration
- [src/main/java/com/example/demo/health/TaskHealthIndicator.java](src/main/java/com/example/demo/health/TaskHealthIndicator.java) — custom Actuator health contributor

## Runtime Configuration

The service is configured with these default runtime values:

- Server port: `8087`
- Application name: `demo`
- JPA/Hibernate `ddl-auto: update`
- SQL logging enabled: `show-sql: true`
- H2 console enabled at `/h2-console`
- H2 JDBC URL: `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- H2 database username: `sa`
- H2 password: empty

## Build and Run

### Build

```bash
mvn clean package
```

### Run locally

```bash
mvn spring-boot:run
```

### Or run the packaged jar

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Local service address

The application binds to:

```text
http://localhost:8087
```

## Exposed REST API

Base path: `/api/tasks`

### 1. List all tasks

- Method: `GET`
- Path: `/api/tasks`
- Response: JSON array of `Task`

Request example:

```bash
curl http://localhost:8087/api/tasks
```

### 2. Read one task by ID

- Method: `GET`
- Path: `/api/tasks/{id}`
- Response: a single `Task` JSON object

Example:

```bash
curl http://localhost:8087/api/tasks/1
```

### 3. Create a task

- Method: `POST`
- Path: `/api/tasks/newtask`
- Body: JSON task payload
- Response: `201 Created` with the saved task

Example:

```bash
curl -X POST http://localhost:8087/api/tasks/newtask \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Buy milk",
        "description": "2 liters",
        "completed": false
      }'
```

Example payload:

```json
{
  "title": "Buy milk",
  "description": "2 liters",
  "completed": false
}
```

### 4. Update one task

- Method: `PUT`
- Path: `/api/tasks/{id}`
- Body: JSON task payload
- Response: updated `Task`

Example:

```bash
curl -X PUT http://localhost:8087/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Buy milk",
        "description": "2 liters",
        "completed": true
      }'
```

### 5. Bulk update tasks asynchronously

- Method: `PUT`
- Path: `/api/tasks/bulk`
- Body: JSON array of tasks that includes an `id` for every element
- Response: `200 OK` with a list of updated tasks
- Execution: asynchronous `CompletableFuture` via a configured executor

Example:

```bash
curl -X PUT http://localhost:8087/api/tasks/bulk \
  -H "Content-Type: application/json" \
  -d '[
        { "id": 1, "title": "Task A", "description": "Updated", "completed": true },
        { "id": 2, "title": "Task B", "description": "Updated", "completed": false }
      ]'
```

### 6. Delete one task

- Method: `DELETE`
- Path: `/api/tasks/{id}`
- Response: `204 No Content`

Example:

```bash
curl -X DELETE http://localhost:8087/api/tasks/1
```

## Error Model

All error responses are serialized as JSON through the global exception handler and follow a common structure.

Example response shape:

```json
{
  "timestamp": "2026-08-08T12:34:56.789",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id 99",
  "path": "/api/tasks/99"
}
```

### Error mapping

- `ResourceNotFoundException` → HTTP `404 Not Found`
- `IllegalArgumentException` → HTTP `400 Bad Request`
- `RequestNotPermitted` / rate-limiter rejection → HTTP `429 Too Many Requests`
- `BulkUpdateException` → HTTP `500 Internal Server Error`
- Generic exceptions → HTTP `500 Internal Server Error`

## Caching

The service uses Spring Cache annotations:

- `getAllTasks()` is cached in the `tasks` cache
- `getTask(id)` is cached in the `task` cache keyed by the id
- `createTask()` evicts the `tasks` cache
- `updateTask()` evicts `tasks` and updates/refreshes the `task` cache entry
- `deleteTask()` evicts both the `tasks` and `task` cache entries

## Rate Limiting

The `POST /api/tasks/newtask` endpoint is protected by a Resilience4j rate limiter configured with the following defaults:

- Limit: `3` requests per period
- Refresh period: `1m`
- Timeout: `0`
- Instance name: `createTaskRateLimiter`

If the limit is exceeded, the API returns a `429 Too Many Requests` response shape from the configured fallback routine.

## Actuator and Health Endpoints

The application exposes selected Actuator endpoints over HTTP:

```text
GET /actuator/health
GET /actuator/health/readiness
GET /actuator/health/liveness
GET /actuator/info
GET /actuator/metrics
GET /actuator/prometheus
GET /actuator/loggers
GET /actuator/threaddump
GET /actuator/beans
```

You can browse them from the management server on the same base port:

```text
http://localhost:8087/actuator
```

### Health detail

The custom `TaskHealthIndicator` checks repository reachability by executing `taskRepository.count()` and reports an `UP` or `DOWN` health status with DB/repository detail metadata.

## H2 Console

The embedded H2 console is enabled and can be opened at:

```text
http://localhost:8087/h2-console
```

Use the JDBC URL and credentials from the datasource section of the configuration file:

- JDBC URL: `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Username: `sa`
- Password: blank

## Data Persistence

Because the datasource is configured as an in-memory H2 database, data is not persisted across application restarts. The application uses Hibernate schema generation with `ddl-auto: update`.

## Notes

- The SQL generation and JPA metadata are configured through the YAML file.
- The application is not a multi-tenant or multi-module system; it is a small sample service for CRUD and observability patterns.
- The `bulkUpdateTasks` method is asynchronous and therefore returns a `CompletableFuture` from the controller.

