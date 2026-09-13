package com.kisansarthi.common;

public class SlotClosedException extends BusinessException {
    public SlotClosedException(String message) {
        super("SLOT_CLOSED", message);
    }
}
