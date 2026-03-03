package com.trimly.enums;
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    REJECTED,
    CANCELLED,
    RESCHEDULE_REQUESTED   // barber requested a new slot; customer yet to respond
}
