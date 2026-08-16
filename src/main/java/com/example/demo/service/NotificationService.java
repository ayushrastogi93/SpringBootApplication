package com.example.demo.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ServerErrorException;

import com.example.demo.model.TaskDao;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Web.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@FeignClient(name = "notification-publisher", url = "http://localhost:8088/")
interface NotificationClient {
    @PostMapping("v1/publish")
    void publishTaskCreated(@RequestBody TaskDao notification);
}


@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationClient notificationClient;

    @Autowired
    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public void publishTaskCreated(TaskDao notification) {
        logger.info("Notifying downstream service about new task creation: {}", notification);
        try {
            notificationClient.publishTaskCreated(notification);
            logger.info("Task creation notification sent successfully");
        } catch (ServerErrorException e) {
            logger.error("Downstream service is unavailable. Failed to send task creation notification", e);
        } catch (Exception e) {
            logger.error("Failed to send task creation notification", e);
        }
    }
}
