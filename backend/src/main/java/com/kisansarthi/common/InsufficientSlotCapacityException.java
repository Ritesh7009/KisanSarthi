package com.kisansarthi.common;

public class InsufficientSlotCapacityException extends BusinessException {
    public InsufficientSlotCapacityException(String message) {
        super("INSUFFICIENT_SLOT_CAPACITY", message);
    }
}
