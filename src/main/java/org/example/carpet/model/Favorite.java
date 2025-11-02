package org.example.carpet.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("favorites")
public class Favorite {
    @Id
    private String id;

    @Indexed
    private String userEmail;

    @Indexed
    private String sku;

    private long createdAtTs;
}
