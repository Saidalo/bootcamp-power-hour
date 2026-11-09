package com.bootcamp.powerhour.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, Object id) {
        this(resource + " with id " + id + " was not found");
    }
}
