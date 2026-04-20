package com.petgym.exception;

public class MembershipExpiredException extends RuntimeException {
    public MembershipExpiredException(String message) {
        super(message);
    }
}
