package com.example.demo.health;

import com.example.demo.repository.TaskRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class TaskHealthIndicator implements HealthIndicator {

    private final TaskRepository taskRepository;

    public TaskHealthIndicator(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Health health() {
        try {
            taskRepository.count();
            return Health.up()
                    .withDetail("database", "h2")
                    .withDetail("repository", "tasks")
                    .withDetail("status", "reachable")
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("database", "h2")
                    .withDetail("repository", "tasks")
                    .withDetail("status", "unreachable")
                    .build();
        }
    }
}
