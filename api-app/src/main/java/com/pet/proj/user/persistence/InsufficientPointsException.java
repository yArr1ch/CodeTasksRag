package com.pet.proj.user.persistence;

public class InsufficientPointsException extends IllegalStateException {
    public InsufficientPointsException() {
        super("not enough points");
    }
}
