package com.petgym.domain;

// enum статусов бронирования тренировки
public enum BookingStatus {
    PENDING,               // ожидает подтверждения
    CONFIRMED,             // подтверждено (тренер принял)
    CANCELLED_BY_CLIENT,   // отменено клиентом
    CANCELLED_BY_TRAINER,  // отменено тренером
    COMPLETED              // тренировка состоялась
}
