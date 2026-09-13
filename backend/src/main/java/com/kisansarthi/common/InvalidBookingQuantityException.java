package com.kisansarthi.common;

public class InvalidBookingQuantityException extends BusinessException {
    public InvalidBookingQuantityException(String message) {
        super("INVALID_BOOKING_QUANTITY", message);
    }
}
