package com.kisansarthi.booking;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum BookingStatus {
    BOOKED,
    GATE_CALLED,
    GATE_ENTERED,
    WEIGHING,
    QUALITY_CHECK,
    WEIGHMENT_COMPLETED,
    PROCUREMENT_COMPLETED,
    PAYMENT_PENDING,
    PAYMENT_INITIATED,
    PAYMENT_CREDITED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.of(
            BOOKED, EnumSet.of(GATE_CALLED, CANCELLED),
            GATE_CALLED, EnumSet.of(GATE_ENTERED, BOOKED, CANCELLED),
            GATE_ENTERED, EnumSet.of(WEIGHING, CANCELLED),
            WEIGHING, EnumSet.of(QUALITY_CHECK, REJECTED),
            QUALITY_CHECK, EnumSet.of(WEIGHMENT_COMPLETED, REJECTED),
            WEIGHMENT_COMPLETED, EnumSet.of(PROCUREMENT_COMPLETED),
            PROCUREMENT_COMPLETED, EnumSet.of(PAYMENT_PENDING),
            PAYMENT_PENDING, EnumSet.of(PAYMENT_INITIATED),
            PAYMENT_INITIATED, EnumSet.of(PAYMENT_CREDITED),
            PAYMENT_CREDITED, EnumSet.of(COMPLETED),
            COMPLETED, Collections.emptySet(),
            CANCELLED, Collections.emptySet(),
            REJECTED, Collections.emptySet()
    );

    public boolean canTransitionTo(BookingStatus target) {
        Set<BookingStatus> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
