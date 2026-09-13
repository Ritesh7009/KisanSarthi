package com.kisansarthi.common;

public class InvalidBookingDateException extends BusinessException {
    public InvalidBookingDateException(String message) {
        super("INVALID_BOOKING_DATE", message);
    }
}
