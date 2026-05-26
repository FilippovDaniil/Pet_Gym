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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private AlfaBankGatewayClient gatewayClient;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private MembershipTypeRepository membershipTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipService membershipService;

    @InjectMocks private PaymentService paymentService;

    private User client;
    private MembershipType type;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "returnUrl", "http://localhost:30091/api/payment/callback");
        ReflectionTestUtils.setField(paymentService, "failUrl", "http://localhost:30091/api/payment/fail");

        client = User.builder().id(1L).email("ivan@test.com")
                .firstName("Иван").lastName("Петров")
                .role(Role.CLIENT).enabled(true).build();

        type = MembershipType.builder().id(2L).name("1 месяц — безлимит")
                .durationDays(30).price(new BigDecimal("2500")).isActive(true).build();
    }

    // ─── initiatePayment ──────────────────────────────────────────────────────

    @Test
    void initiatePayment_success_returnsFormUrl() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(membershipTypeRepository.findById(2L)).thenReturn(Optional.of(type));

        RegisterOrderResponse gwResponse = new RegisterOrderResponse();
        gwResponse.setOrderId("alfa-uuid-123");
        gwResponse.setFormUrl("https://alfa.rbsuat.com/payment/merchants/test?mdOrder=alfa-uuid-123");
        when(gatewayClient.registerOrder(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(gwResponse);
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String formUrl = paymentService.initiatePayment(1L, 2L);

        assertNotNull(formUrl);
        assertTrue(formUrl.contains("alfa.rbsuat.com"));
        verify(paymentOrderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void initiatePayment_clientNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.initiatePayment(99L, 2L));
        verifyNoInteractions(gatewayClient);
    }

    @Test
    void initiatePayment_typeNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(membershipTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.initiatePayment(1L, 99L));
        verifyNoInteractions(gatewayClient);
    }

    @Test
    void initiatePayment_inactiveType_throwsBusinessException() {
        type.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(membershipTypeRepository.findById(2L)).thenReturn(Optional.of(type));

        assertThrows(BusinessException.class, () -> paymentService.initiatePayment(1L, 2L));
        verifyNoInteractions(gatewayClient);
    }

    @Test
    void initiatePayment_gatewayReturnsError_throwsBusinessException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(membershipTypeRepository.findById(2L)).thenReturn(Optional.of(type));

        RegisterOrderResponse gwResponse = new RegisterOrderResponse();
        gwResponse.setErrorCode("5");
        gwResponse.setErrorMessage("Доступ запрещён");
        when(gatewayClient.registerOrder(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(gwResponse);

        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.initiatePayment(1L, 2L));
        assertTrue(ex.getMessage().contains("Доступ запрещён"));
        verifyNoInteractions(paymentOrderRepository);
    }

    @Test
    void initiatePayment_amountConvertedToKopecks() {
        // 2500 руб = 250000 копеек
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(membershipTypeRepository.findById(2L)).thenReturn(Optional.of(type));

        RegisterOrderResponse gwResponse = new RegisterOrderResponse();
        gwResponse.setOrderId("alfa-uuid");
        gwResponse.setFormUrl("https://alfa.rbsuat.com/form");
        when(gatewayClient.registerOrder(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(gwResponse);
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        paymentService.initiatePayment(1L, 2L);

        verify(gatewayClient).registerOrder(anyString(), eq(250000L), anyString(), anyString(), anyString());
    }

    // ─── confirmPayment ───────────────────────────────────────────────────────

    @Test
    void confirmPayment_paid_activatesMembership() {
        PaymentOrder order = buildPendingOrder();
        when(paymentOrderRepository.findByAlfaOrderId("alfa-uuid-123")).thenReturn(Optional.of(order));

        OrderStatusResponse statusResp = new OrderStatusResponse();
        statusResp.setErrorCode("0");
        statusResp.setOrderStatus(2); // DEPOSITED = успешная оплата
        when(gatewayClient.getOrderStatusExtended("alfa-uuid-123")).thenReturn(statusResp);

        PurchaseDto purchase = PurchaseDto.builder()
                .id(10L).typeName("1 месяц — безлимит")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(30))
                .paidAmount(new BigDecimal("2500")).build();
        when(membershipService.buyMembership(eq(1L), eq(2L), any())).thenReturn(purchase);
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseDto result = paymentService.confirmPayment("alfa-uuid-123");

        assertNotNull(result);
        assertEquals("1 месяц — безлимит", result.getTypeName());
        assertEquals(PaymentOrderStatus.PAID, order.getStatus());
        assertNotNull(order.getPaidAt());
        verify(membershipService).buyMembership(1L, 2L, LocalDate.now());
    }

    @Test
    void confirmPayment_orderNotFound_throwsResourceNotFoundException() {
        when(paymentOrderRepository.findByAlfaOrderId("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.confirmPayment("unknown"));
        verifyNoInteractions(gatewayClient);
    }

    @Test
    void confirmPayment_alreadyPaid_throwsBusinessException() {
        PaymentOrder order = buildPendingOrder();
        order.setStatus(PaymentOrderStatus.PAID);
        when(paymentOrderRepository.findByAlfaOrderId("alfa-uuid-123")).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> paymentService.confirmPayment("alfa-uuid-123"));
        verifyNoInteractions(gatewayClient);
    }

    @Test
    void confirmPayment_gatewayStatusError_throwsBusinessException() {
        PaymentOrder order = buildPendingOrder();
        when(paymentOrderRepository.findByAlfaOrderId("alfa-uuid-123")).thenReturn(Optional.of(order));

        OrderStatusResponse statusResp = new OrderStatusResponse();
        statusResp.setErrorCode("7");
        statusResp.setErrorMessage("Системная ошибка");
        when(gatewayClient.getOrderStatusExtended("alfa-uuid-123")).thenReturn(statusResp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.confirmPayment("alfa-uuid-123"));
        assertTrue(ex.getMessage().contains("Системная ошибка"));
        verifyNoInteractions(membershipService);
    }

    @Test
    void confirmPayment_declined_setsFailedStatus() {
        PaymentOrder order = buildPendingOrder();
        when(paymentOrderRepository.findByAlfaOrderId("alfa-uuid-123")).thenReturn(Optional.of(order));

        OrderStatusResponse statusResp = new OrderStatusResponse();
        statusResp.setErrorCode("0");
        statusResp.setOrderStatus(6); // DECLINED
        when(gatewayClient.getOrderStatusExtended("alfa-uuid-123")).thenReturn(statusResp);
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(BusinessException.class, () -> paymentService.confirmPayment("alfa-uuid-123"));
        assertEquals(PaymentOrderStatus.FAILED, order.getStatus());
        verifyNoInteractions(membershipService);
    }

    @Test
    void confirmPayment_cancelled_setsCancelledStatus() {
        PaymentOrder order = buildPendingOrder();
        when(paymentOrderRepository.findByAlfaOrderId("alfa-uuid-123")).thenReturn(Optional.of(order));

        OrderStatusResponse statusResp = new OrderStatusResponse();
        statusResp.setErrorCode("0");
        statusResp.setOrderStatus(3); // REVERSED
        when(gatewayClient.getOrderStatusExtended("alfa-uuid-123")).thenReturn(statusResp);
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(BusinessException.class, () -> paymentService.confirmPayment("alfa-uuid-123"));
        assertEquals(PaymentOrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void confirmPayment_stillPending_throwsBusinessException() {
        PaymentOrder order = buildPendingOrder();
        when(paymentOrderRepository.findByAlfaOrderId("alfa-uuid-123")).thenReturn(Optional.of(order));

        OrderStatusResponse statusResp = new OrderStatusResponse();
        statusResp.setErrorCode("0");
        statusResp.setOrderStatus(0); // CREATED — ещё не оплачено
        when(gatewayClient.getOrderStatusExtended("alfa-uuid-123")).thenReturn(statusResp);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.confirmPayment("alfa-uuid-123"));
        assertTrue(ex.getMessage().contains("Оплата не завершена"));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PaymentOrder buildPendingOrder() {
        return PaymentOrder.builder()
                .id(5L)
                .orderNumber("PG-ABCD12345678")
                .alfaOrderId("alfa-uuid-123")
                .client(client)
                .membershipType(type)
                .amountKopecks(250000L)
                .status(PaymentOrderStatus.PENDING)
                .build();
    }
}
