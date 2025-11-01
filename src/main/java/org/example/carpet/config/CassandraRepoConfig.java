package org.example.carpet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@Profile("cassandra") // 只有激活 cassandra profile 才启用仓库扫描
@EnableCassandraRepositories(basePackages = "org.example.carpet.repository.cassandra")
public class CassandraRepoConfig {
}
