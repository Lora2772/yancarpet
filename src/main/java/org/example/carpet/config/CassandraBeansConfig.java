// src/main/java/org/example/carpet/config/CassandraBeansConfig.java
package org.example.carpet.config;

import java.net.InetSocketAddress;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;

/**
 * 手动注入 Cassandra 必要 Bean，确保有 cassandraTemplate / cassandraOperations 可注入
 * Only active when NOT in test profile
 */
@Configuration
@Profile("!test")
public class CassandraBeansConfig {

    @Bean
    public CqlSession cqlSession() {
        // 与 application.yml 保持一致
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress("localhost", 9042))
                .withLocalDatacenter("datacenter1")
                .withKeyspace("carpet_ks")
                .build();
    }

    @Bean
    public CassandraMappingContext cassandraMappingContext() {
        return new CassandraMappingContext();
    }

    @Bean
    public CassandraConverter cassandraConverter(CassandraMappingContext mappingContext) {
        return new MappingCassandraConverter(mappingContext);
    }

    @Bean
    public CassandraTemplate cassandraTemplate(CqlSession session, CassandraConverter converter) {
        return new CassandraTemplate(session, converter);
    }

}
