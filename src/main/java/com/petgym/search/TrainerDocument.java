package com.petgym.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerDocument {
    private String id;             // _id в OpenSearch — всегда String (в БД Long)
    private String firstName;
    private String lastName;
    private String email;
    private String specialization; // text + keyword (dynamic mapping)
    private String bio;
}
