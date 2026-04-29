package com.petgym.service;

import com.petgym.domain.*;
import com.petgym.dto.PurchaseDto;
import com.petgym.exception.BusinessException;
import com.petgym.repository.MembershipTypeRepository;
import com.petgym.repository.PurchaseRepository;
import com.petgym.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) — подключаем Mockito к JUnit 5
// Mockito позволяет создавать "заглушки" (моки) для зависимостей, чтобы тестировать класс изолированно
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock private MembershipTypeRepository typeRepository;  // мок: имитирует репозиторий, не обращается к БД
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MembershipService membershipService; // тестируемый класс; Mockito внедрит моки через конструктор

    // Тестовые данные, создаваемые перед каждым тестом
    private User client;
    private MembershipType type;

    @BeforeEach // метод выполняется перед каждым тестовым методом
    void setUp() {
        // создаём тестового клиента через builder
        client = User.builder().id(1L).email("test@test.com").firstName("Test").lastName("User")
                .role(Role.CLIENT).enabled(true).build();
        // создаём активный тип абонемента на 30 дней за 2500 руб.
        type = MembershipType.builder().id(1L).name("1 месяц").durationDays(30)
                .price(new BigDecimal("2500")).isActive(true).build();
    }

    @Test // тест успешной покупки абонемента
    void buyMembership_success() {
        // when(...).thenReturn(...) — настройка мока: "когда вызовут этот метод — вернуть это"
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(typeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(purchaseRepository.findActivePurchases(anyLong(), any())).thenReturn(List.of()); // нет активных

        // thenAnswer — возвращаем сохранённый объект с заполненным id (имитируем БД)
        when(purchaseRepository.save(any())).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0); // берём переданный аргумент
            p = Purchase.builder().id(10L).client(client).membershipType(type)
                    .startDate(p.getStartDate()).endDate(p.getEndDate())
                    .paidAmount(p.getPaidAmount()).build();
            return p;
        });

        PurchaseDto result = membershipService.buyMembership(1L, 1L, LocalDate.now());

        // assertNotNull — проверяем, что результат не null
        assertNotNull(result);
        // assertEquals — проверяем конкретные значения
        assertEquals(LocalDate.now().plusDays(30), result.getEndDate()); // дата конца = сегодня + 30 дней
        assertEquals(new BigDecimal("2500"), result.getPaidAmount());
        assertTrue(result.isActive()); // абонемент должен быть активен
    }

    @Test // тест: попытка купить деактивированный тип → должно бросить BusinessException
    void buyMembership_inactiveType_throwsException() {
        type.setActive(false); // деактивируем тип
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(typeRepository.findById(1L)).thenReturn(Optional.of(type));

        // assertThrows — проверяем, что метод бросает нужное исключение
        assertThrows(BusinessException.class,
                () -> membershipService.buyMembership(1L, 1L, LocalDate.now()));
    }

    @Test // тест: hasActiveMembership возвращает true, если есть активная покупка
    void hasActiveMembership_true() {
        Purchase active = Purchase.builder().id(1L).client(client).membershipType(type)
                .startDate(LocalDate.now().minusDays(5))   // начался 5 дней назад
                .endDate(LocalDate.now().plusDays(25))      // заканчивается через 25 дней
                .paidAmount(new BigDecimal("2500")).build();
        when(purchaseRepository.findActivePurchases(1L, LocalDate.now())).thenReturn(List.of(active));

        assertTrue(membershipService.hasActiveMembership(1L, LocalDate.now())); // ожидаем true
    }

    @Test // тест: hasActiveMembership возвращает false, если нет активных покупок
    void hasActiveMembership_false() {
        when(purchaseRepository.findActivePurchases(1L, LocalDate.now())).thenReturn(List.of()); // пустой список

        assertFalse(membershipService.hasActiveMembership(1L, LocalDate.now())); // ожидаем false
    }
}
