package com.example.demo.controller;

import com.example.demo.dto.CreateTaskRequestV2;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.model.Task;
import com.example.demo.service.TaskService;
import com.example.demo.service.NotificationService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService; 
    private final NotificationService notificationService; // Inject the notification service
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    public TaskController(TaskService taskService, NotificationService notificationService) {
        this.taskService = taskService;
        this.notificationService = notificationService;
    }

    @GetMapping("/tasks")
    public List<Task> getAllTasks() {
        logger.info("Received request to get all tasks");
        return taskService.getAllTasks();
    }

    @GetMapping("/tasks/{id}")
    public Task getTaskById(@PathVariable Long id) {
        logger.info("Received request to get task by ID: {}", id);
        return taskService.getTask(id);
    }


    @PostMapping(path = {"/tasks", "/v1/tasks"})
    @RateLimiter(name = "createTaskRateLimiter", fallbackMethod = "createTaskFallback")
    public ResponseEntity<Object> createTask(@RequestBody Task task) {
        logger.info("Received request to create task: {}", task);
        if(task.getTitle() == "13") {
            logger.warn("Attempted to create a task with restricted title: {}", task.getTitle());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("New task cannot have a restricted title.");
        }
        Task created = taskService.createTask(task);
        // Inject a service to notify the downstream service about the new task creation
        notificationService.notifyTaskCreated(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/v2/tasks")
    @RateLimiter(name = "createTaskRateLimiter", fallbackMethod = "createTaskFallback")
    public ResponseEntity<Object> createTaskV2(@RequestBody CreateTaskRequestV2 request) {
        logger.info("Received request to create task: {}", request);
        Task task = new Task(request.getName(), request.getDetails(), request.isDone());
        Task created = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    public ResponseEntity<Object> createTaskFallback(Task task, RequestNotPermitted ex) {
        logger.warn("Rate limit exceeded for createTask");
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
        logger.info("Received request to update task: {}", task);
        return taskService.updateTask(id, task);
    }

    @PutMapping("/tasks/bulk")
    public CompletableFuture<ResponseEntity<List<Task>>> bulkUpdateTasks(@RequestBody List<Task> tasks) {
        logger.info("Received request to bulk update tasks: {}", tasks);
        return taskService.bulkUpdateTasks(tasks)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        logger.info("Received request to delete task: {}", id);
        taskService.deleteTask(id);
    }
}
