package com.petgym.search;

import java.util.List;

public interface TrainerSearchService {
    void indexTrainer(TrainerDocument doc);
    void removeTrainer(String id);
    List<TrainerDocument> search(String query, String specialization, int page, int size);
    void reindexAll(List<TrainerDocument> docs);
}
