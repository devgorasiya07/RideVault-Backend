package com.ridevault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a ride state transition violates the state machine rules.
 *
 * Resume: "Orchestrated ride state machine workflows"
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRideStateException extends RuntimeException {

    public InvalidRideStateException(String message) {
        super(message);
    }
}
