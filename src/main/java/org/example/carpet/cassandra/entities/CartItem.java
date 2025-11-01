package org.example.carpet.cassandra.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.carpet.cassandra.keys.CartItemKey;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Cassandra 表：cart_items_by_user
 * 主键复合：user_email + sku
 *
 * 对应 CQL：
 * CREATE TABLE IF NOT EXISTS cart_items_by_user (
 *   user_email text,
 *   sku text,
 *   qty int,
 *   price decimal,
 *   updated_at_ts bigint,
 *   PRIMARY KEY (user_email, sku)
 * );
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("cart_items_by_user")
public class CartItem {

    /** 复合主键，包含 user_email + sku */
    @PrimaryKey
    private CartItemKey key;

    /** 购物车数量 */
    @Column("qty")
    private Integer qty;

    /** 单价 */
    @Column("price")
    private BigDecimal price;

    /** 更新时间戳（毫秒） */
    @Column("updated_at_ts")
    private Long updatedAtTs;
}
