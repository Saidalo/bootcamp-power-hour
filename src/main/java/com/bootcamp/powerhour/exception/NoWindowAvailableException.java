package com.bootcamp.powerhour.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the appliance cannot fit anywhere inside the window its owner allows.
 *
 * @author Karyna Litvinova
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class NoWindowAvailableException extends RuntimeException {

    public NoWindowAvailableException(String message) {
        super(message);
    }
}