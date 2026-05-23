# SpringBootApplication

A Spring Boot application demonstrating:

- CRUD operations for `Task` entities
- JPA persistence with H2 in-memory database
- caching for `getAllTasks()` and individual task lookups
- Resilience4j rate limiting for `createTask`
- global exception handling with structured JSON error responses
- YAML-based application configuration

## Project structure

- `src/main/java/com/example/demo/DemoApplication.java` — application entry point
- `src/main/java/com/example/demo/controller/TaskController.java` — REST API layer
- `src/main/java/com/example/demo/service/TaskService.java` — service layer with caching
- `src/main/java/com/example/demo/repository/TaskRepository.java` — Spring Data JPA repository
- `src/main/java/com/example/demo/model/Task.java` — JPA entity
- `src/main/java/com/example/demo/exception/` — exception handling classes
- `src/main/resources/application.yaml` — application and Resilience4j configuration

## Requirements

- Java 17+
- Maven 3.8+

## Build and run

1. Build the project:
   ```bash
   mvn clean package
   ```

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

3. The service runs on:
   ```bash
   http://localhost:8080
   ```

## API endpoints

### Get all tasks

```bash
curl http://localhost:8080/api/tasks
```

### Get a task by id

```bash
curl http://localhost:8080/api/tasks/1
```

### Create a task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"2 liters","completed":false}'
```

### Update a task

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"2 liters","completed":true}'
```

### Delete a task

```bash
curl -X DELETE http://localhost:8080/api/tasks/1
```

## Caching behavior

- `getAllTasks()` is cached under `tasks`
- `getTask(id)` is cached under `task`
- `createTask()`, `updateTask()`, and `deleteTask()` evict the `tasks` cache
- `updateTask()` also refreshes the individual `task` cache entry
- `deleteTask()` evicts both the list and individual task cache entries

## Rate limiting

- `POST /api/tasks` is protected by Resilience4j rate limiting
- limit: `3` requests per minute
- requests beyond the limit return HTTP `429 Too Many Requests`
- fallback returns a structured error JSON response

## Error handling

The application returns structured JSON error responses for:

- `404 Not Found` when a task does not exist
- `400 Bad Request` for invalid input
- `429 Too Many Requests` when the rate limit is exceeded
- `500 Internal Server Error` for unexpected failures

## H2 console

You can access the embedded H2 database console at:

```bash
http://localhost:8080/h2-console
```

Use the default JDBC URL and credentials from `application.yaml`.

## Notes

- Configuration is now managed in `src/main/resources/application.yaml`.
- The application uses in-memory H2 database, so data is not persisted after restart.
