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

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock private MembershipTypeRepository typeRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MembershipService membershipService;

    private User client;
    private MembershipType type;

    @BeforeEach
    void setUp() {
        client = User.builder().id(1L).email("test@test.com").firstName("Test").lastName("User")
                .role(Role.CLIENT).enabled(true).build();
        type = MembershipType.builder().id(1L).name("1 месяц").durationDays(30)
                .price(new BigDecimal("2500")).isActive(true).build();
    }

    @Test
    void buyMembership_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(typeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(purchaseRepository.findActivePurchases(anyLong(), any())).thenReturn(List.of());
        when(purchaseRepository.save(any())).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            p = Purchase.builder().id(10L).client(client).membershipType(type)
                    .startDate(p.getStartDate()).endDate(p.getEndDate())
                    .paidAmount(p.getPaidAmount()).build();
            return p;
        });

        PurchaseDto result = membershipService.buyMembership(1L, 1L, LocalDate.now());

        assertNotNull(result);
        assertEquals(LocalDate.now().plusDays(30), result.getEndDate());
        assertEquals(new BigDecimal("2500"), result.getPaidAmount());
        assertTrue(result.isActive());
    }

    @Test
    void buyMembership_inactiveType_throwsException() {
        type.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(typeRepository.findById(1L)).thenReturn(Optional.of(type));

        assertThrows(BusinessException.class,
                () -> membershipService.buyMembership(1L, 1L, LocalDate.now()));
    }

    @Test
    void hasActiveMembership_true() {
        Purchase active = Purchase.builder().id(1L).client(client).membershipType(type)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(25))
                .paidAmount(new BigDecimal("2500")).build();
        when(purchaseRepository.findActivePurchases(1L, LocalDate.now())).thenReturn(List.of(active));

        assertTrue(membershipService.hasActiveMembership(1L, LocalDate.now()));
    }

    @Test
    void hasActiveMembership_false() {
        when(purchaseRepository.findActivePurchases(1L, LocalDate.now())).thenReturn(List.of());
        assertFalse(membershipService.hasActiveMembership(1L, LocalDate.now()));
    }
}
