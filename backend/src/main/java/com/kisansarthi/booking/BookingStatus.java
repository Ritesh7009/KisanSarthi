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
    WEIGHMENT_STAGE_1,
    QUALITY_CHECK,
    WEIGHMENT_STAGE_2,
    WEIGHMENT_COMPLETED,
    PROCUREMENT_COMPLETED,
    PAYMENT_PENDING,
    PAYMENT_INITIATED,
    PAYMENT_PROCESSING,
    PAYMENT_CREDITED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry(BOOKED, EnumSet.of(GATE_CALLED, CANCELLED)),
            Map.entry(GATE_CALLED, EnumSet.of(GATE_ENTERED, BOOKED, CANCELLED)),
            Map.entry(GATE_ENTERED, EnumSet.of(WEIGHING, CANCELLED)),
            Map.entry(WEIGHING, EnumSet.of(WEIGHMENT_STAGE_1, QUALITY_CHECK, REJECTED)),
            Map.entry(WEIGHMENT_STAGE_1, EnumSet.of(QUALITY_CHECK, WEIGHMENT_STAGE_2, REJECTED)),
            Map.entry(QUALITY_CHECK, EnumSet.of(WEIGHMENT_STAGE_2, WEIGHMENT_COMPLETED, REJECTED)),
            Map.entry(WEIGHMENT_STAGE_2, EnumSet.of(WEIGHMENT_COMPLETED, PROCUREMENT_COMPLETED)),
            Map.entry(WEIGHMENT_COMPLETED, EnumSet.of(PROCUREMENT_COMPLETED)),
            Map.entry(PROCUREMENT_COMPLETED, EnumSet.of(PAYMENT_PENDING)),
            Map.entry(PAYMENT_PENDING, EnumSet.of(PAYMENT_INITIATED)),
            Map.entry(PAYMENT_INITIATED, EnumSet.of(PAYMENT_PROCESSING, PAYMENT_CREDITED)),
            Map.entry(PAYMENT_PROCESSING, EnumSet.of(PAYMENT_CREDITED, COMPLETED)),
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
