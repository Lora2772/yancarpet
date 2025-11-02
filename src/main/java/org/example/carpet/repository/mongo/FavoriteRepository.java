package org.example.carpet.repository.mongo;

import java.util.List;
import org.example.carpet.model.Favorite;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FavoriteRepository extends MongoRepository<Favorite, String> {
    List<Favorite> findByUserEmail(String userEmail);
    boolean existsByUserEmailAndSku(String userEmail, String sku);
    void deleteByUserEmailAndSku(String userEmail, String sku);
}
