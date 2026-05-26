package com.petgym.domain;

public enum PaymentOrderStatus {
    PENDING,   // зарегистрирован в Alfa Bank, ожидает оплаты
    PAID,      // оплачен (orderStatus=2), абонемент активирован
    FAILED,    // отклонён банком (orderStatus=6)
    CANCELLED  // отменён (orderStatus=3)
}
