package com.eric6166.taskapi.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
