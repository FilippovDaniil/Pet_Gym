package com.petgym.search;

import com.petgym.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(2) // запускается после DataInitializer (@Order(1)) — тренеры уже созданы
@RequiredArgsConstructor
public class SearchInitializer implements ApplicationRunner {

    private final TrainerRepository trainerRepository;

    // required=false — работает если opensearch.enabled=false
    @Autowired(required = false)
    private TrainerSearchService trainerSearchService;

    @Override
    @Transactional(readOnly = true) // держим JPA-сессию открытой для доступа к Trainer.getUser()
    public void run(ApplicationArguments args) {
        if (trainerSearchService == null) {
            log.info("OpenSearch отключён — пропускаем реиндексацию тренеров");
            return;
        }
        List<TrainerDocument> docs = trainerRepository.findAll().stream()
                .map(t -> TrainerDocument.builder()
                        .id(String.valueOf(t.getUserId()))
                        .firstName(t.getUser().getFirstName())
                        .lastName(t.getUser().getLastName())
                        .email(t.getUser().getEmail())
                        .specialization(t.getSpecialization())
                        .bio(t.getBio())
                        .build())
                .toList();
        trainerSearchService.reindexAll(docs);
        log.info("OpenSearch: реиндексировано {} тренеров при старте", docs.size());
    }
}
