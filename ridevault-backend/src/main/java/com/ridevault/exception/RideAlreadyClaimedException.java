package com.ridevault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when multiple drivers race to accept the same pending ride.
 *
 * Resume: "executing high-concurrency updates via raw SQL optimistic locking
 *          (WHERE status='PENDING') to eradicate double-booking race conditions
 *          under load"
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class RideAlreadyClaimedException extends RuntimeException {

    public RideAlreadyClaimedException(String message) {
        super(message);
    }
}
