package com.kisansarthi.common;

public class CapacityReductionNotAllowedException extends BusinessException {
    public CapacityReductionNotAllowedException(String message) {
        super("CAPACITY_REDUCTION_NOT_ALLOWED", message);
    }
}
