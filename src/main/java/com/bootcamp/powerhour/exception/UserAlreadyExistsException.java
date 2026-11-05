package com.bootcamp.powerhour.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExistsException extends RuntimeException {
    private final String field;

    public UserAlreadyExistsException(String field, String value) {
        super("User with " + field + " " + value + " already exists.");
        this.field = field;
    }
}
