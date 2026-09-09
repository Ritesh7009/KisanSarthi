package com.kisansarthi.common;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String currentStatus, String targetStatus) {
        super("Invalid booking status transition from '" + currentStatus + "' to '" + targetStatus + "'");
    }
}
