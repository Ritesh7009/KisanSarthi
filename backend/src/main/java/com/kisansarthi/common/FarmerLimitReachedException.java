package com.kisansarthi.common;

public class FarmerLimitReachedException extends BusinessException {
    public FarmerLimitReachedException(String message) {
        super("FARMER_LIMIT_REACHED", message);
    }
}
