package com.petgym.service;

import com.petgym.domain.*;
import com.petgym.dto.PurchaseDto;
import com.petgym.exception.BusinessException;
import com.petgym.exception.ResourceNotFoundException;
import com.petgym.integration.alfabank.AlfaBankGatewayClient;
import com.petgym.integration.alfabank.dto.OrderStatusResponse;
import com.petgym.integration.alfabank.dto.RegisterOrderResponse;
import com.petgym.repository.MembershipTypeRepository;
import com.petgym.repository.PaymentOrderRepository;
import com.petgym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AlfaBankGatewayClient gatewayClient;
    private final PaymentOrderRepository paymentOrderRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;

    @Value("${alfabank.return-url}")
    private String returnUrl;

    @Value("${alfabank.fail-url}")
    private String failUrl;

    /**
     * Инициирует оплату абонемента через Alfa Bank.
     * Возвращает URL страницы оплаты банка.
     */
    @Transactional
    public String initiatePayment(Long clientId, Long membershipTypeId) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", clientId));
        MembershipType type = membershipTypeRepository.findById(membershipTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", membershipTypeId));

        if (!type.isActive()) {
            throw new BusinessException("Тип абонемента недоступен для покупки");
        }

        long amountKopecks = type.getPrice()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        String orderNumber = "PG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String description = "Абонемент: " + type.getName();

        RegisterOrderResponse response = gatewayClient.registerOrder(
                orderNumber, amountKopecks, returnUrl, failUrl, description);

        if (!response.isSuccess()) {
            log.error("[PAYMENT] event=REGISTER_FAILED clientId={} typeId={} errorCode={} errorMessage={}",
                    clientId, membershipTypeId, response.getErrorCode(), response.getErrorMessage());
            throw new BusinessException("Ошибка платёжного шлюза: " + response.getErrorMessage());
        }

        PaymentOrder order = PaymentOrder.builder()
                .orderNumber(orderNumber)
                .alfaOrderId(response.getOrderId())
                .client(client)
                .membershipType(type)
                .amountKopecks(amountKopecks)
                .status(PaymentOrderStatus.PENDING)
                .build();
        paymentOrderRepository.save(order);

        log.info("[PAYMENT] event=INITIATED clientId={} typeId=\"{}\" orderNumber={} alfaOrderId={}",
                clientId, type.getName(), orderNumber, response.getOrderId());

        return response.getFormUrl();
    }

    /**
     * Обрабатывает callback от Alfa Bank (redirect браузера после оплаты).
     * Проверяет статус, при успехе активирует абонемент.
     * Возвращает PurchaseDto если оплата прошла, иначе бросает BusinessException.
     */
    @Transactional
    public PurchaseDto confirmPayment(String alfaOrderId) {
        PaymentOrder order = paymentOrderRepository.findByAlfaOrderId(alfaOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder not found for alfaOrderId: " + alfaOrderId));

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            throw new BusinessException("Заказ уже оплачен");
        }

        OrderStatusResponse statusResponse = gatewayClient.getOrderStatusExtended(alfaOrderId);

        if (!"0".equals(statusResponse.getErrorCode())) {
            log.error("[PAYMENT] event=STATUS_ERROR alfaOrderId={} error={}", alfaOrderId, statusResponse.getErrorMessage());
            throw new BusinessException("Ошибка получения статуса платежа: " + statusResponse.getErrorMessage());
        }

        if (statusResponse.isPaid()) {
            order.setStatus(PaymentOrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            paymentOrderRepository.save(order);

            log.info("[PAYMENT] event=PAID clientId={} orderNumber={} alfaOrderId={}",
                    order.getClient().getId(), order.getOrderNumber(), alfaOrderId);

            PurchaseDto purchase = membershipService.buyMembership(
                    order.getClient().getId(),
                    order.getMembershipType().getId(),
                    LocalDate.now());

            log.info("[PAYMENT] event=MEMBERSHIP_ACTIVATED purchaseId={} clientId={} type=\"{}\"",
                    purchase.getId(), order.getClient().getId(), order.getMembershipType().getName());

            return purchase;
        }

        if (statusResponse.isFailed()) {
            order.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);
            log.warn("[PAYMENT] event=DECLINED clientId={} orderNumber={}", order.getClient().getId(), order.getOrderNumber());
            throw new BusinessException("Платёж отклонён банком");
        }

        if (statusResponse.isCancelled()) {
            order.setStatus(PaymentOrderStatus.CANCELLED);
            paymentOrderRepository.save(order);
            throw new BusinessException("Платёж отменён");
        }

        // Статус PENDING — ещё не оплачено (например, клиент вернулся до оплаты)
        throw new BusinessException("Оплата не завершена. Статус: " + statusResponse.getOrderStatus());
    }
}
