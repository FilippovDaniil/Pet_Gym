package com.petgym.service;

import com.petgym.domain.User;
import com.petgym.domain.Visit;
import com.petgym.dto.VisitDto;
import com.petgym.exception.BusinessException;
import com.petgym.exception.MembershipExpiredException;
import com.petgym.exception.ResourceNotFoundException;
import com.petgym.repository.UserRepository;
import com.petgym.repository.VisitRepository;
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
public class VisitService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;

    @Transactional
    public VisitDto markVisit(Long clientId, Long markedByUserId) {
        LocalDate today = LocalDate.now();
        if (!membershipService.hasActiveMembership(clientId, today)) {
            throw new MembershipExpiredException("У клиента нет активного абонемента");
        }
        if (visitRepository.existsByClientIdAndVisitDate(clientId, today)) {
            throw new BusinessException("Посещение уже отмечено сегодня");
        }
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        User markedBy = userRepository.findById(markedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", markedByUserId));

        Visit visit = Visit.builder()
                .client(client)
                .visitDate(today)
                .markedBy(markedBy)
                .build();
        visit = visitRepository.save(visit);
        log.info("Visit marked for client {} by {}", clientId, markedByUserId);
        return toDto(visit);
    }

    @Transactional(readOnly = true)
    public List<VisitDto> getTodayVisits() {
        return visitRepository.findByVisitDate(LocalDate.now())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private VisitDto toDto(Visit v) {
        String markedByName = v.getMarkedBy() != null
                ? v.getMarkedBy().getFirstName() + " " + v.getMarkedBy().getLastName() : "";
        return VisitDto.builder()
                .id(v.getId())
                .clientId(v.getClient().getId())
                .clientName(v.getClient().getFirstName() + " " + v.getClient().getLastName())
                .clientEmail(v.getClient().getEmail())
                .visitDate(v.getVisitDate())
                .markedByName(markedByName)
                .markedAt(v.getMarkedAt())
                .build();
    }
}
