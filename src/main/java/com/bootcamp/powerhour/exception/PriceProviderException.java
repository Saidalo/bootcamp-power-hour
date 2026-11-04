package com.bootcamp.powerhour.exception;

public class PriceProviderException extends RuntimeException {
    public PriceProviderException(String message) {
        super(message);
    }

    public PriceProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
