package com.example.demo.service;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void notifyTaskCreated(Object Notification) {
        // Implement the logic to notify the downstream service about the new task creation
        // For example, you can use RestTemplate or WebClient to send an HTTP request to the downstream service
        logger.info("Notifying downstream service about new task creation: {}", Notification);
        // Example: sendNotificationToDownstreamService(Notification);
    }

}
