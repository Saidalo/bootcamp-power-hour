package com.bootcamp.powerhour.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ZoneNotFoundException extends RuntimeException {
    public ZoneNotFoundException(String code) {
        super("Bidding zone not found: " + code);
    }
}
