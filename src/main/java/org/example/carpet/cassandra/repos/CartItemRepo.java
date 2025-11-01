package org.example.carpet.cassandra.repos;

import java.util.List;
import org.example.carpet.cassandra.entities.CartItem;
import org.example.carpet.cassandra.keys.CartItemKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Cassandra repository for cart items.
 *
 * CQL table:
 *   CREATE TABLE IF NOT EXISTS cart_items_by_user (
 *       user_email text,
 *       sku text,
 *       qty int,
 *       price decimal,
 *       updated_at_ts bigint,
 *       PRIMARY KEY (user_email, sku)
 *   );
 */
@Repository
public interface CartItemRepo extends CassandraRepository<CartItem, CartItemKey> {

    /** 查找指定用户的购物车商品 */
    @Query("SELECT * FROM cart_items_by_user WHERE user_email = :email")
    List<CartItem> findByUserEmail(@Param("email") String email);

    /** 删除指定用户的某个 SKU */
    @Query("DELETE FROM cart_items_by_user WHERE user_email = :email AND sku = :sku")
    void deleteByUserAndSku(@Param("email") String email, @Param("sku") String sku);

    /** 清空该用户的购物车 */
    @Query("DELETE FROM cart_items_by_user WHERE user_email = :email")
    void deleteAllByUser(@Param("email") String email);
}
