package com.example.demo.controller;

import com.example.demo.dto.CreateTaskRequestV2;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.model.Task;
import com.example.demo.service.TaskService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/tasks/{id}")
    public Task getTaskById(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PostMapping(path = {"/tasks", "/v1/tasks"})
    @RateLimiter(name = "createTaskRateLimiter", fallbackMethod = "createTaskFallback")
    public ResponseEntity<Object> createTask(@RequestBody Task task) {
        Task created = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/v2/tasks")
    @RateLimiter(name = "createTaskRateLimiter", fallbackMethod = "createTaskFallback")
    public ResponseEntity<Object> createTaskV2(@RequestBody CreateTaskRequestV2 request) {
        Task task = new Task(request.getName(), request.getDetails(), request.isDone());
        Task created = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    public ResponseEntity<Object> createTaskFallback(Task task, RequestNotPermitted ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Rate limit exceeded: createTask is limited to 3 requests per minute.",
                "/api/v1/tasks"
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @PutMapping("/tasks/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody Task task) {
        return taskService.updateTask(id, task);
    }

    @PutMapping("/tasks/bulk")
    public CompletableFuture<ResponseEntity<List<Task>>> bulkUpdateTasks(@RequestBody List<Task> tasks) {
        return taskService.bulkUpdateTasks(tasks)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }
}
