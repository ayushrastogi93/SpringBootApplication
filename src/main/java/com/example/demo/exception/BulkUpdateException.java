package com.example.demo.exception;

import java.util.Collections;
import java.util.List;

public class BulkUpdateException extends RuntimeException {

    private final List<String> errors;

    public BulkUpdateException(String message, List<String> errors) {
        super(message);
        this.errors = errors == null ? Collections.emptyList() : errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
