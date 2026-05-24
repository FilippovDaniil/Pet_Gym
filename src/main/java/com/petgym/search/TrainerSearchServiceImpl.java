package com.petgym.search;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class TrainerSearchServiceImpl implements TrainerSearchService {

    private static final String INDEX = "trainers";

    // Паттерн B: бин null когда opensearch.enabled=false (ConditionalOnProperty не создаёт его)
    @Autowired(required = false)
    private OpenSearchClient client;

    @PostConstruct
    public void ensureIndex() {
        if (client == null) return;
        try {
            boolean exists = client.indices().exists(r -> r.index(INDEX)).value();
            if (!exists) {
                client.indices().create(r -> r.index(INDEX));
                log.info("OpenSearch index '{}' создан", INDEX);
            }
        } catch (Exception e) {
            log.warn("OpenSearch недоступен при старте: {}", e.getMessage());
        }
    }

    @Override
    public void indexTrainer(TrainerDocument doc) {
        if (client == null) return;
        try {
            client.index(r -> r.index(INDEX).id(doc.getId()).document(doc));
        } catch (Exception e) {
            log.warn("OpenSearch: ошибка индексирования тренера id={}: {}", doc.getId(), e.getMessage());
        }
    }

    @Override
    public void removeTrainer(String id) {
        if (client == null) return;
        try {
            client.delete(r -> r.index(INDEX).id(id));
        } catch (Exception e) {
            log.warn("OpenSearch: ошибка удаления тренера id={}: {}", id, e.getMessage());
        }
    }

    @Override
    public List<TrainerDocument> search(String query, String specialization, int page, int size) {
        if (client == null) return Collections.emptyList();
        try {
            List<Query> clauses = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                // multi_match: ищет по имени (приоритет ^2), специализации и описанию
                // fuzziness AUTO — допускает опечатки
                clauses.add(Query.of(q -> q.multiMatch(m -> m
                        .fields("firstName^2", "lastName^2", "specialization", "bio")
                        .query(query)
                        .fuzziness("AUTO"))));
            }
            if (specialization != null && !specialization.isBlank()) {
                // ⚠️ MatchQuery.query() принимает FieldValue, не String (opensearch-java 2.x)
                clauses.add(Query.of(q -> q.match(m -> m
                        .field("specialization")
                        .query(FieldValue.of(specialization)))));
            }

            Query finalQuery = clauses.isEmpty()
                    ? Query.of(q -> q.matchAll(m -> m))
                    : Query.of(q -> q.bool(b -> b.must(clauses)));

            SearchRequest request = new SearchRequest.Builder()
                    .index(INDEX)
                    .from(page * size)
                    .size(size)
                    .query(finalQuery)
                    .build();

            SearchResponse<TrainerDocument> response = client.search(request, TrainerDocument.class);

            return response.hits().hits().stream()
                    .map(h -> h.source())
                    .filter(d -> d != null)
                    .toList();
        } catch (Exception e) {
            log.warn("OpenSearch: поиск не удался: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void reindexAll(List<TrainerDocument> docs) {
        if (client == null) return;
        docs.forEach(this::indexTrainer);
        log.info("OpenSearch: реиндексировано {} тренеров", docs.size());
    }
}
