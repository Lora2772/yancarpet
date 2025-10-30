package org.example.carpet.repository;

import org.example.carpet.model.UserAccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserAccountRepository extends MongoRepository<UserAccountDocument, String> {

    Optional<UserAccountDocument> findByEmailIgnoreCase(String email);
}
