package com.petgym.integration.alfabank.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderStatusResponse {

    private String errorCode;
    private String errorMessage;
    private String orderNumber;
    private Integer orderStatus;
    private Long amount;
    private String currency;
    private Long date;

    public boolean isPaid() {
        return Integer.valueOf(2).equals(orderStatus);
    }

    public boolean isFailed() {
        return Integer.valueOf(6).equals(orderStatus);
    }

    public boolean isCancelled() {
        return Integer.valueOf(3).equals(orderStatus);
    }
}
