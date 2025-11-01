package org.example.carpet.cassandra.keys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

/**
 * 复合主键：user_email + sku
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class CartItemKey {

    @PrimaryKeyColumn(name = "user_email", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String userEmail;

    @PrimaryKeyColumn(name = "sku", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String sku;
}
