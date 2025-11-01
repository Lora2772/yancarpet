-- =========================================================
--  Keyspace
-- =========================================================
CREATE KEYSPACE IF NOT EXISTS carpet_ks
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE carpet_ks;

-- =========================================================
-- 1. 库存预留（Inventory Reservation）
-- =========================================================
-- 按 SKU 查询最近预留（高并发写入）
CREATE TABLE IF NOT EXISTS inventory_reservations_by_sku (
                                                             sku text,
                                                             reserved_at_ts bigint,
                                                             order_id text,
                                                             qty int,
                                                             PRIMARY KEY (sku, reserved_at_ts)
) WITH CLUSTERING ORDER BY (reserved_at_ts DESC);

-- 按订单查询预留（辅助表）
CREATE TABLE IF NOT EXISTS inventory_reservations_by_order (
                                                               order_id text,
                                                               reserved_at_ts bigint,
                                                               sku text,
                                                               qty int,
                                                               PRIMARY KEY (order_id, reserved_at_ts)
) WITH CLUSTERING ORDER BY (reserved_at_ts DESC);

-- =========================================================
-- 2. 订单事件时间线（Order Event Timeline）
-- =========================================================
CREATE TABLE IF NOT EXISTS order_events_by_order (
                                                     order_id text,
                                                     ts bigint,
                                                     type text,
                                                     payload_json text,
                                                     PRIMARY KEY (order_id, ts)
) WITH CLUSTERING ORDER BY (ts DESC);

-- 示例行：
-- ('ORD-12345', 1730367290000, 'OrderCreated', '{"email":"user@xx.com"}')
-- ('ORD-12345', 1730367300000, 'InventoryReserved', '{"sku":"rug1","qty":2}')

-- =========================================================
-- 3. 购物车（Cart）
-- =========================================================
CREATE TABLE IF NOT EXISTS cart_items_by_user (
                                                  user_email text,
                                                  sku text,
                                                  qty int,
                                                  price decimal,
                                                  updated_at_ts bigint,
                                                  PRIMARY KEY (user_email, sku)
);
-- 默认 TTL 可选：
-- ALTER TABLE cart_items_by_user WITH default_time_to_live = 2592000; -- 30 天

-- =========================================================
-- 可选: 查看表 TTL 设置
-- SELECT table_name, default_time_to_live FROM system_schema.tables WHERE keyspace_name='carpet_ks';
-- 购物车（分区：user_email；去重：sku）
CREATE TABLE IF NOT EXISTS cart_items (
                                          user_email text,
                                          sku text,
                                          name text,
                                          price decimal,
                                          qty int,
                                          image_url text,
                                          room_type list<text>,
                                          keywords list<text>,
                                          updated_at_ts bigint,
                                          PRIMARY KEY (user_email, sku)
);
