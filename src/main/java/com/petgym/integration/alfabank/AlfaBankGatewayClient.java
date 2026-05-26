package com.petgym.integration.alfabank;

import com.petgym.integration.alfabank.dto.OrderStatusResponse;
import com.petgym.integration.alfabank.dto.RegisterOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class AlfaBankGatewayClient {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final String username;
    private final String password;

    public AlfaBankGatewayClient(
            RestTemplate restTemplate,
            @Value("${alfabank.gateway.url}") String gatewayUrl,
            @Value("${alfabank.api.username}") String username,
            @Value("${alfabank.api.password}") String password) {
        this.restTemplate = restTemplate;
        this.gatewayUrl = gatewayUrl;
        this.username = username;
        this.password = password;
    }

    public RegisterOrderResponse registerOrder(String orderNumber, long amountKopecks,
                                               String returnUrl, String failUrl,
                                               String description) {
        MultiValueMap<String, String> params = buildAuthParams();
        params.add("orderNumber", orderNumber);
        params.add("amount", String.valueOf(amountKopecks));
        params.add("returnUrl", returnUrl);
        if (failUrl != null) params.add("failUrl", failUrl);
        if (description != null) params.add("description", description);

        log.info("[PAYMENT] event=REGISTER_ORDER orderNumber={} amount={}", orderNumber, amountKopecks);
        RegisterOrderResponse response = post("register.do", params, RegisterOrderResponse.class);
        log.info("[PAYMENT] event=REGISTER_ORDER_RESPONSE orderId={} errorCode={}", response.getOrderId(), response.getErrorCode());
        return response;
    }

    public OrderStatusResponse getOrderStatusExtended(String alfaOrderId) {
        MultiValueMap<String, String> params = buildAuthParams();
        params.add("orderId", alfaOrderId);

        log.info("[PAYMENT] event=GET_STATUS alfaOrderId={}", alfaOrderId);
        OrderStatusResponse response = post("getOrderStatusExtended.do", params, OrderStatusResponse.class);
        log.info("[PAYMENT] event=GET_STATUS_RESPONSE alfaOrderId={} orderStatus={}", alfaOrderId, response.getOrderStatus());
        return response;
    }

    private MultiValueMap<String, String> buildAuthParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("userName", username);
        params.add("password", password);
        return params;
    }

    private <T> T post(String method, MultiValueMap<String, String> params, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        return restTemplate.postForObject(gatewayUrl + method, request, responseType);
    }
}
