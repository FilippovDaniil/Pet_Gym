package com.petgym.service;

import com.petgym.domain.MembershipType;
import com.petgym.domain.Purchase;
import com.petgym.domain.User;
import com.petgym.dto.MembershipTypeDto;
import com.petgym.dto.PurchaseDto;
import com.petgym.exception.BusinessException;
import com.petgym.exception.ResourceNotFoundException;
import com.petgym.repository.MembershipTypeRepository;
import com.petgym.repository.PurchaseRepository;
import com.petgym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipTypeRepository typeRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    public boolean hasActiveMembership(Long clientId, LocalDate date) {
        return !purchaseRepository.findActivePurchases(clientId, date).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<MembershipTypeDto> getAllActiveTypes() {
        return typeRepository.findByIsActiveTrue().stream()
                .map(this::toTypeDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MembershipTypeDto> getAllTypes() {
        return typeRepository.findAll().stream()
                .map(this::toTypeDto).collect(Collectors.toList());
    }

    @Transactional
    public MembershipTypeDto createType(MembershipTypeDto dto) {
        MembershipType type = MembershipType.builder()
                .name(dto.getName())
                .durationDays(dto.getDurationDays())
                .price(dto.getPrice())
                .isActive(true)
                .build();
        type = typeRepository.save(type);
        log.info("Created membership type: {}", type.getName());
        return toTypeDto(type);
    }

    @Transactional
    public MembershipTypeDto updateType(Long id, MembershipTypeDto dto) {
        MembershipType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", id));
        type.setName(dto.getName());
        type.setDurationDays(dto.getDurationDays());
        type.setPrice(dto.getPrice());
        type.setActive(dto.isActive());
        log.info("Updated membership type id={}", id);
        return toTypeDto(typeRepository.save(type));
    }

    @Transactional
    public void deleteType(Long id) {
        MembershipType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", id));
        type.setActive(false);
        typeRepository.save(type);
        log.info("Deactivated membership type id={}", id);
    }

    @Transactional
    public PurchaseDto buyMembership(Long clientId, Long typeId, LocalDate startDate) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", clientId));
        MembershipType type = typeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", typeId));
        if (!type.isActive()) {
            throw new BusinessException("Тип абонемента неактивен");
        }

        boolean hasActive = hasActiveMembership(clientId, startDate);
        if (hasActive) {
            log.info("Client {} already has active membership, adding a new one (overlap allowed)", clientId);
        }

        LocalDate endDate = startDate.plusDays(type.getDurationDays());
        Purchase purchase = Purchase.builder()
                .client(client)
                .membershipType(type)
                .startDate(startDate)
                .endDate(endDate)
                .paidAmount(type.getPrice())
                .build();
        purchase = purchaseRepository.save(purchase);
        log.info("Purchase created: clientId={}, typeId={}, start={}, end={}", clientId, typeId, startDate, endDate);
        return toPurchaseDto(purchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDto> getClientPurchases(Long clientId) {
        return purchaseRepository.findByClientIdWithType(clientId).stream()
                .map(this::toPurchaseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PurchaseDto> getActiveMemberships() {
        LocalDate today = LocalDate.now();
        return purchaseRepository.findAll().stream()
                .filter(p -> !p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today))
                .map(this::toPurchaseDto)
                .collect(Collectors.toList());
    }

    private MembershipTypeDto toTypeDto(MembershipType t) {
        return MembershipTypeDto.builder()
                .id(t.getId())
                .name(t.getName())
                .durationDays(t.getDurationDays())
                .price(t.getPrice())
                .active(t.isActive())
                .build();
    }

    private PurchaseDto toPurchaseDto(Purchase p) {
        LocalDate today = LocalDate.now();
        return PurchaseDto.builder()
                .id(p.getId())
                .clientId(p.getClient().getId())
                .clientName(p.getClient().getFirstName() + " " + p.getClient().getLastName())
                .typeId(p.getMembershipType().getId())
                .typeName(p.getMembershipType().getName())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .paidAmount(p.getPaidAmount())
                .paymentDate(p.getPaymentDate())
                .active(!p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today))
                .build();
    }
}
