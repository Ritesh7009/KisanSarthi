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

    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry(BOOKED, EnumSet.of(GATE_CALLED, CANCELLED)),
            Map.entry(GATE_CALLED, EnumSet.of(GATE_ENTERED, BOOKED, CANCELLED)),
            Map.entry(GATE_ENTERED, EnumSet.of(WEIGHING, CANCELLED)),
            Map.entry(WEIGHING, EnumSet.of(QUALITY_CHECK, REJECTED)),
            Map.entry(QUALITY_CHECK, EnumSet.of(WEIGHMENT_COMPLETED, REJECTED)),
            Map.entry(WEIGHMENT_COMPLETED, EnumSet.of(PROCUREMENT_COMPLETED)),
            Map.entry(PROCUREMENT_COMPLETED, EnumSet.of(PAYMENT_PENDING)),
            Map.entry(PAYMENT_PENDING, EnumSet.of(PAYMENT_INITIATED)),
            Map.entry(PAYMENT_INITIATED, EnumSet.of(PAYMENT_CREDITED)),
            Map.entry(PAYMENT_CREDITED, EnumSet.of(COMPLETED)),
            Map.entry(COMPLETED, Collections.emptySet()),
            Map.entry(CANCELLED, Collections.emptySet()),
            Map.entry(REJECTED, Collections.emptySet())
    );

    public boolean canTransitionTo(BookingStatus target) {
        Set<BookingStatus> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
