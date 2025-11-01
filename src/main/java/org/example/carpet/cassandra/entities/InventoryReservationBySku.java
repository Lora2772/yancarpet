package org.example.carpet.cassandra.entities;

import org.example.carpet.cassandra.keys.InventoryReservationBySkuKey;
import org.springframework.data.cassandra.core.mapping.*;

@Table("inventory_reservations_by_sku")
public class InventoryReservationBySku {

    @PrimaryKey
    private InventoryReservationBySkuKey key;

    @Column("order_id")
    private String orderId;

    @Column("qty")
    private Integer qty;

    public InventoryReservationBySku() {}
    public InventoryReservationBySku(InventoryReservationBySkuKey key, String orderId, Integer qty) {
        this.key = key;
        this.orderId = orderId;
        this.qty = qty;
    }
    public InventoryReservationBySkuKey getKey() { return key; }
    public void setKey(InventoryReservationBySkuKey key) { this.key = key; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
}
