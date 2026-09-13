package com.kisansarthi.common;

public class BookingAlreadyCancelledException extends BusinessException {
    public BookingAlreadyCancelledException(String message) {
        super("BOOKING_ALREADY_CANCELLED", message);
    }
}
