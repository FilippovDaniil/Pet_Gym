package com.petgym.controller;

import com.petgym.dto.PurchaseDto;
import com.petgym.exception.BusinessException;
import com.petgym.security.JwtTokenProvider;
import com.petgym.security.UserDetailsServiceImpl;
import com.petgym.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// addFilters=false: callback — публичный endpoint, JWT-фильтры не нужны.
// JwtTokenProvider и UserDetailsServiceImpl мокаем, чтобы JwtAuthenticationFilter смог создаться в контексте.
@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private PaymentService paymentService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsServiceImpl;

    @Test
    void callback_successfulPayment_returnsHtmlWithMembershipDetails() throws Exception {
        PurchaseDto purchase = PurchaseDto.builder()
                .id(10L)
                .typeName("1 месяц — безлимит")
                .startDate(LocalDate.of(2026, 5, 26))
                .endDate(LocalDate.of(2026, 6, 25))
                .paidAmount(new BigDecimal("2500.00"))
                .build();
        when(paymentService.confirmPayment("alfa-uuid-123")).thenReturn(purchase);

        mockMvc.perform(get("/api/payment/callback")
                        .param("orderId", "alfa-uuid-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("Оплата прошла успешно")))
                .andExpect(content().string(containsString("1 месяц — безлимит")))
                .andExpect(content().string(containsString("2500.00")));
    }

    @Test
    void callback_businessException_returnsHtmlWithFailMessage() throws Exception {
        when(paymentService.confirmPayment("bad-order"))
                .thenThrow(new BusinessException("Платёж отклонён банком"));

        mockMvc.perform(get("/api/payment/callback")
                        .param("orderId", "bad-order"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("Оплата не прошла")))
                .andExpect(content().string(containsString("Платёж отклонён банком")));
    }

    @Test
    void callback_unexpectedException_returnsGenericFailHtml() throws Exception {
        when(paymentService.confirmPayment("crash-order"))
                .thenThrow(new RuntimeException("NPE"));

        mockMvc.perform(get("/api/payment/callback")
                        .param("orderId", "crash-order"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("Оплата не прошла")));
    }

    @Test
    void fail_returnsFailHtml() throws Exception {
        mockMvc.perform(get("/api/payment/fail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("Оплата не прошла")))
                .andExpect(content().string(containsString("Оплата не была завершена")));
    }

    @Test
    void fail_withOrderId_returnsFailHtml() throws Exception {
        mockMvc.perform(get("/api/payment/fail")
                        .param("orderId", "some-order"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Оплата не прошла")));
    }
}
