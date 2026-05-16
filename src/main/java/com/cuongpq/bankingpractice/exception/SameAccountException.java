package com.cuongpq.bankingpractice.exception;

public class SameAccountException extends RuntimeException {

    public SameAccountException(String message) {
        super(message);
    }
}
