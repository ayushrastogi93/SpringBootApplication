package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Task;
import com.example.demo.repository.TaskRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Cacheable("tasks")
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Cacheable(value = "task", key = "#id")
    public Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    @CacheEvict(value = "tasks", allEntries = true)
    public Task createTask(Task task) {
        task.setId(null);
        return taskRepository.save(task);
    }

    @CacheEvict(value = "tasks", allEntries = true)
    @CachePut(value = "task", key = "#id")
    public Task updateTask(Long id, Task updatedTask) {
        Task existing = getTask(id);
        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setCompleted(updatedTask.isCompleted());
        return taskRepository.save(existing);
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
