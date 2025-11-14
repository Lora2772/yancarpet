package org.example.carpet;

import org.example.carpet.cassandra.repos.CartItemRepo;
import org.example.carpet.kafka.InventoryEventProducer;
import org.example.carpet.kafka.PaymentEventProducer;
import org.example.carpet.repository.mongo.AccountRepository;
import org.example.carpet.repository.mongo.FavoriteRepository;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.example.carpet.repository.mongo.OrderRepository;
import org.example.carpet.repository.mongo.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
                // Disable Cassandra repositories for tests
                "spring.data.cassandra.repositories.enabled=false"
        }
)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        // Exclude MongoDB, Cassandra and Kafka - only JPA/Hibernate will run with H2
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        CassandraAutoConfiguration.class,
        CassandraDataAutoConfiguration.class,
        CassandraRepositoriesAutoConfiguration.class,
        CassandraReactiveRepositoriesAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
class CarpetApplicationTests {

    // Mock Kafka producers
    @MockBean private PaymentEventProducer paymentEventProducer;
    @MockBean private InventoryEventProducer inventoryEventProducer;

    // Mock MongoDB dependencies
    @MockBean private MongoTemplate mongoTemplate;
    @MockBean private AccountRepository accountRepository;
    @MockBean private FavoriteRepository favoriteRepository;
    @MockBean private ItemDocumentRepository itemDocumentRepository;
    @MockBean private OrderRepository orderRepository;
    @MockBean private PaymentRepository paymentRepository;

    // Mock Cassandra dependencies
    @MockBean private CassandraTemplate cassandraTemplate;
    @MockBean private CartItemRepo cartItemRepo;
    @MockBean private org.example.carpet.cassandra.repos.InventoryReservationByOrderRepo inventoryReservationByOrderRepo;
    @MockBean private org.example.carpet.cassandra.repos.InventoryReservationBySkuRepo inventoryReservationBySkuRepo;
    @MockBean private org.example.carpet.cassandra.repos.OrderEventRepo orderEventRepo;
    @MockBean private org.example.carpet.cassandra.ShipmentEventRepository shipmentEventRepository;

    // Mock Security dependencies
    @MockBean private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        // Context loads successfully with JPA/H2 enabled and MongoDB/Cassandra/Kafka mocked
    }
}
