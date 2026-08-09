package com.example.demo.service;

import com.example.demo.exception.BulkUpdateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Task;
import com.example.demo.repository.TaskRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final Executor taskBulkExecutor;
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    public TaskService(TaskRepository taskRepository, @Qualifier("taskBulkExecutor") Executor taskBulkExecutor) {
        this.taskRepository = taskRepository;
        this.taskBulkExecutor = taskBulkExecutor;
    }

    @Cacheable("tasks")
    public List<Task> getAllTasks() {
        logger.info("Fetching all tasks from the database");
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskOptional(Long id) {
        logger.info("Fetching task with ID: {}", id);
        return taskRepository.findById(id);
    }

    @Cacheable(value = "task", key = "#id")
    public Task getTask(Long id) {
        logger.info("Fetching task with ID: {}", id);
        return getTaskOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    @CacheEvict(value = "tasks", allEntries = true)
    public Task createTask(Task task) {
        logger.info("Creating task: {}", task);
        task.setId(null);
        return taskRepository.save(task);
    }

    @CacheEvict(value = "tasks", allEntries = true)
    @CachePut(value = "task", key = "#id")
    public Task updateTask(Long id, Task updatedTask) {
        logger.info("Updating task with ID: {}", id);
        return applyUpdateTask(id, updatedTask);
    }

    @Async("taskBulkExecutor")
    public CompletableFuture<List<Task>> bulkUpdateTasks(List<Task> tasks) {
        logger.info("Initiating bulk update for {} tasks", tasks.size());
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Task list must not be empty for bulk update.");
        }

        List<CompletableFuture<Task>> futures = new ArrayList<>(tasks.size());
        for (Task task : tasks) {
            futures.add(CompletableFuture.supplyAsync(() -> updateTaskSafely(task), taskBulkExecutor));
        }

        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allDone.thenApply(v -> {
            List<Task> updatedTasks = new ArrayList<>(tasks.size());
            List<String> errors = new ArrayList<>();

            for (CompletableFuture<Task> future : futures) {
                try {
                    updatedTasks.add(future.join());
                } catch (CompletionException ex) {
                    errors.add(unwrap(ex).getMessage());
                }
            }

            if (!errors.isEmpty()) {
                throw new BulkUpdateException("Bulk update completed with errors.", errors);
            }

            return updatedTasks;
        });
    }

    private Task updateTaskSafely(Task task) {
        logger.info("Updating task safely: {}", task);
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("Each task must include an id for bulk update.");
        }
        return applyUpdateTask(task.getId(), task);
    }

    private Task applyUpdateTask(Long id, Task updatedTask) {
        Task existing = getTask(id);
        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setCompleted(updatedTask.isCompleted());
        return taskRepository.save(existing);
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return unwrap(throwable.getCause());
        }
        return throwable;
    }

    @Caching(evict = {
            @CacheEvict(value = "tasks", allEntries = true),
            @CacheEvict(value = "task", key = "#id")
    })
    public void deleteTask(Long id) {
        Task existing = getTask(id);
        taskRepository.delete(existing);
    }
}
