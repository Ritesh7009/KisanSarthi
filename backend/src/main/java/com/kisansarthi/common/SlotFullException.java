package com.kisansarthi.common;

public class SlotFullException extends BusinessException {
    public SlotFullException(String message) {
        super("SLOT_FULL", message);
    }
}
