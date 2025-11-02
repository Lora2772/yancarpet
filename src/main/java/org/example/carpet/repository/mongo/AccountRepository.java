package org.example.carpet.repository.mongo;

import org.example.carpet.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByEmail(String email);
}
