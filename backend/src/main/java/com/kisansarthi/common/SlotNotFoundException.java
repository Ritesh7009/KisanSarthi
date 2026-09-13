package com.kisansarthi.common;

public class SlotNotFoundException extends BusinessException {
    public SlotNotFoundException(String message) {
        super("SLOT_NOT_FOUND", message);
    }
}
