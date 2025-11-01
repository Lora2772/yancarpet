package org.example.carpet;

import org.example.carpet.kafka.PaymentEventProducer;
import org.example.carpet.repository.jpa.PaymentLedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
                // 关键：关闭 Cassandra 仓库自动装配（即使有 starter 也不启用）
                "spring.data.cassandra.repositories.enabled=false"
        }
)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        CassandraAutoConfiguration.class,
        CassandraDataAutoConfiguration.class,
        // 这两个是专门负责“启用 Cassandra Repositories”的自动配置，顺手也排了
        CassandraRepositoriesAutoConfiguration.class,
        CassandraReactiveRepositoriesAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
class CarpetApplicationTests {

    @MockBean private PaymentLedgerRepository paymentLedgerRepository;
    @MockBean private PaymentEventProducer paymentEventProducer;

    @Test
    void contextLoads() {}
}
