package com.bootcamp.powerhour.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when no prices have been imported for the period being planned.
 *
 * @author Karyna Litvinova
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PricesNotAvailableException extends RuntimeException {

    public PricesNotAvailableException(String message) {
        super(message);
    }
}