package com.petgym.controller;

import com.petgym.search.TrainerDocument;
import com.petgym.search.TrainerSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Полнотекстовый поиск (OpenSearch)")
public class SearchController {

    // required=false — работает даже если opensearch.enabled=false (бин не создан)
    @Autowired(required = false)
    private TrainerSearchService trainerSearchService;

    // GET /api/search/trainers?q=&specialization=&page=&size=
    // Доступен без авторизации (permitAll в SecurityConfig)
    @GetMapping("/trainers")
    @Operation(summary = "Полнотекстовый поиск тренеров по имени, специализации или описанию")
    public ResponseEntity<List<TrainerDocument>> searchTrainers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (trainerSearchService == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(trainerSearchService.search(q, specialization, page, size));
    }
}
