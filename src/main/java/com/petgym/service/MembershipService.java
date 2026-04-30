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

    // Проверить, есть ли у клиента действующий абонемент на указанную дату
    // Используется в BookingService и VisitService перед записью/отметкой посещения
    public boolean hasActiveMembership(Long clientId, LocalDate date) {
        return !purchaseRepository.findActivePurchases(clientId, date).isEmpty();
    }

    // Получить все активные виды абонементов (для отображения клиенту)
    @Transactional(readOnly = true)
    public List<MembershipTypeDto> getAllActiveTypes() {
        return typeRepository.findByIsActiveTrue().stream()
                .map(this::toTypeDto).collect(Collectors.toList()); // преобразуем каждый entity в DTO
    }

    // Получить все виды (в т.ч. деактивированные) — для администратора
    @Transactional(readOnly = true)
    public List<MembershipTypeDto> getAllTypes() {
        return typeRepository.findAll().stream()
                .map(this::toTypeDto).collect(Collectors.toList());
    }

    // Создать новый вид абонемента (администратор)
    @Transactional
    public MembershipTypeDto createType(MembershipTypeDto dto) {
        MembershipType type = MembershipType.builder()
                .name(dto.getName())
                .durationDays(dto.getDurationDays())
                .price(dto.getPrice())
                .isActive(true) // новый тип сразу активен
                .build();
        type = typeRepository.save(type);
        log.info("[ADMIN] event=MEMBERSHIP_TYPE_CREATED typeId={} name=\"{}\" durationDays={} price={}",
                type.getId(), type.getName(), type.getDurationDays(), type.getPrice());
        return toTypeDto(type);
    }

    // Обновить существующий вид абонемента (администратор)
    @Transactional
    public MembershipTypeDto updateType(Long id, MembershipTypeDto dto) {
        MembershipType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", id));
        type.setName(dto.getName());
        type.setDurationDays(dto.getDurationDays());
        type.setPrice(dto.getPrice());
        type.setActive(dto.isActive());
        log.info("[ADMIN] event=MEMBERSHIP_TYPE_UPDATED typeId={} name=\"{}\" price={} active={}",
                id, dto.getName(), dto.getPrice(), dto.isActive());
        return toTypeDto(typeRepository.save(type));
    }

    // "Удалить" тип абонемента — фактически деактивируем (не удаляем из БД, чтобы не сломать историю)
    @Transactional
    public void deleteType(Long id) {
        MembershipType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", id));
        type.setActive(false);
        typeRepository.save(type);
        log.info("[ADMIN] event=MEMBERSHIP_TYPE_DEACTIVATED typeId={} name=\"{}\"", id, type.getName());
    }

    // Оформить покупку абонемента клиенту (ресепшен или сам клиент)
    @Transactional
    public PurchaseDto buyMembership(Long clientId, Long typeId, LocalDate startDate) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", clientId));
        MembershipType type = typeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipType", typeId));

        if (!type.isActive()) {
            log.warn("[WARN] event=BUY_MEMBERSHIP_REJECTED clientId={} typeId={} reason=\"тип деактивирован\"", clientId, typeId);
            throw new BusinessException("Тип абонемента неактивен");
        }

        if (hasActiveMembership(clientId, startDate)) {
            log.info("[CLIENT] event=MEMBERSHIP_OVERLAP clientId={} clientEmail={} typeId={} note=\"уже есть активный, продление разрешено\"",
                    clientId, client.getEmail(), typeId);
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
        log.info("[CLIENT] event=MEMBERSHIP_BOUGHT purchaseId={} clientId={} clientEmail={} typeName=\"{}\" amount={} from={} to={}",
                purchase.getId(), clientId, client.getEmail(), type.getName(), type.getPrice(), startDate, endDate);
        return toPurchaseDto(purchase);
    }

    // Получить все покупки конкретного клиента
    @Transactional(readOnly = true)
    public List<PurchaseDto> getClientPurchases(Long clientId) {
        return purchaseRepository.findByClientIdWithType(clientId).stream() // JOIN FETCH с типом
                .map(this::toPurchaseDto).collect(Collectors.toList());
    }

    // Получить все сейчас действующие абонементы (для ресепшена)
    @Transactional(readOnly = true)
    public List<PurchaseDto> getActiveMemberships() {
        LocalDate today = LocalDate.now();
        return purchaseRepository.findAll().stream()
                .filter(p -> !p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today)) // startDate <= today <= endDate
                .map(this::toPurchaseDto)
                .collect(Collectors.toList());
    }

    // Entity → DTO для вида абонемента
    private MembershipTypeDto toTypeDto(MembershipType t) {
        return MembershipTypeDto.builder()
                .id(t.getId())
                .name(t.getName())
                .durationDays(t.getDurationDays())
                .price(t.getPrice())
                .active(t.isActive())
                .build();
    }

    // Entity → DTO для покупки
    private PurchaseDto toPurchaseDto(Purchase p) {
        LocalDate today = LocalDate.now();
        return PurchaseDto.builder()
                .id(p.getId())
                .clientId(p.getClient().getId())
                .clientName(p.getClient().getFirstName() + " " + p.getClient().getLastName()) // склеиваем имя
                .typeId(p.getMembershipType().getId())
                .typeName(p.getMembershipType().getName())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .paidAmount(p.getPaidAmount())
                .paymentDate(p.getPaymentDate())
                .active(!p.getStartDate().isAfter(today) && !p.getEndDate().isBefore(today)) // вычисляем активность
                .build();
    }
}
