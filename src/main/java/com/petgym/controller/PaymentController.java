package com.petgym.controller;

import com.petgym.dto.PurchaseDto;
import com.petgym.exception.BusinessException;
import com.petgym.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Обработка платёжных callback-ов")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Callback — браузер клиента попадает сюда после успешной оплаты на форме Alfa Bank.
     * orderId передаётся банком как параметр returnUrl?orderId=...
     */
    @GetMapping(value = "/callback", produces = "text/html;charset=UTF-8")
    @Operation(summary = "Callback после оплаты (Alfa Bank redirect)")
    public ResponseEntity<String> paymentCallback(@RequestParam String orderId) {
        try {
            PurchaseDto purchase = paymentService.confirmPayment(orderId);
            String html = buildSuccessHtml(purchase);
            return ResponseEntity.ok().contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)).body(html);
        } catch (BusinessException e) {
            log.warn("[PAYMENT] event=CALLBACK_FAILED orderId={} reason={}", orderId, e.getMessage());
            String html = buildFailHtml(e.getMessage());
            return ResponseEntity.ok().contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)).body(html);
        } catch (Exception e) {
            log.error("[PAYMENT] event=CALLBACK_ERROR orderId={}", orderId, e);
            String html = buildFailHtml("Внутренняя ошибка. Обратитесь в поддержку.");
            return ResponseEntity.ok().contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)).body(html);
        }
    }

    /**
     * Страница неудачной оплаты — браузер попадает сюда если клиент отказался от оплаты.
     */
    @GetMapping(value = "/fail", produces = "text/html;charset=UTF-8")
    @Operation(summary = "Страница неудачной оплаты")
    public ResponseEntity<String> paymentFail(@RequestParam(required = false) String orderId) {
        String html = buildFailHtml("Оплата не была завершена.");
        return ResponseEntity.ok().contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8)).body(html);
    }

    private String buildSuccessHtml(PurchaseDto purchase) {
        return """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Оплата прошла успешно</title>
                  <style>
                    body { font-family: sans-serif; display: flex; justify-content: center;
                           align-items: center; min-height: 100vh; margin: 0; background: #f0faf0; }
                    .card { background: white; border-radius: 12px; padding: 40px;
                            box-shadow: 0 4px 20px rgba(0,0,0,0.1); max-width: 480px; text-align: center; }
                    .icon { font-size: 64px; }
                    h1 { color: #2e7d32; }
                    .info { background: #f5f5f5; border-radius: 8px; padding: 16px;
                            text-align: left; margin: 20px 0; line-height: 1.8; }
                    a { display: inline-block; margin-top: 20px; padding: 12px 28px;
                        background: #2e7d32; color: white; text-decoration: none; border-radius: 8px; }
                    a:hover { background: #1b5e20; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="icon">✅</div>
                    <h1>Оплата прошла успешно!</h1>
                    <div class="info">
                      <strong>Абонемент:</strong> %s<br>
                      <strong>Действует:</strong> %s &mdash; %s<br>
                      <strong>Сумма:</strong> %s ₽
                    </div>
                    <a href="/client.html">Перейти в личный кабинет</a>
                  </div>
                </body>
                </html>
                """.formatted(
                purchase.getTypeName(),
                purchase.getStartDate(),
                purchase.getEndDate(),
                purchase.getPaidAmount().toPlainString()
        );
    }

    private String buildFailHtml(String reason) {
        return """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Ошибка оплаты</title>
                  <style>
                    body { font-family: sans-serif; display: flex; justify-content: center;
                           align-items: center; min-height: 100vh; margin: 0; background: #fff5f5; }
                    .card { background: white; border-radius: 12px; padding: 40px;
                            box-shadow: 0 4px 20px rgba(0,0,0,0.1); max-width: 480px; text-align: center; }
                    .icon { font-size: 64px; }
                    h1 { color: #c62828; }
                    .reason { color: #666; margin: 16px 0; }
                    a { display: inline-block; margin-top: 20px; padding: 12px 28px;
                        background: #c62828; color: white; text-decoration: none; border-radius: 8px; }
                    a:hover { background: #b71c1c; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="icon">❌</div>
                    <h1>Оплата не прошла</h1>
                    <p class="reason">%s</p>
                    <a href="/client.html">Вернуться</a>
                  </div>
                </body>
                </html>
                """.formatted(reason);
    }
}
