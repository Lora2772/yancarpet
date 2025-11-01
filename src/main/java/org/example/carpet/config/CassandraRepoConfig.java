package org.example.carpet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@EnableCassandraRepositories(basePackages = "org.example.carpet.cassandra.repos")
public class CassandraRepoConfig {
    // 不要加 @Profile，避免因为未激活 profile 导致不生效
}
