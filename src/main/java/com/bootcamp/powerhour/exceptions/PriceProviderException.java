package com.bootcamp.powerhour.exceptions;

public class PriceProviderException extends RuntimeException {
    public PriceProviderException(String message) {
        super(message);
    }

    public PriceProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
