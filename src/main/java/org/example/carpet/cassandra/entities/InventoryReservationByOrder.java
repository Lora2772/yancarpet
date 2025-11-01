package org.example.carpet.cassandra.entities;

import org.example.carpet.cassandra.keys.InventoryReservationByOrderKey;
import org.springframework.data.cassandra.core.mapping.*;

@Table("inventory_reservations_by_order")
public class InventoryReservationByOrder {

    @PrimaryKey
    private InventoryReservationByOrderKey key;

    @Column("sku")
    private String sku;

    @Column("qty")
    private Integer qty;

    public InventoryReservationByOrder() {}
    public InventoryReservationByOrder(InventoryReservationByOrderKey key, String sku, Integer qty) {
        this.key = key; this.sku = sku; this.qty = qty;
    }
    public InventoryReservationByOrderKey getKey() { return key; }
    public void setKey(InventoryReservationByOrderKey key) { this.key = key; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
}
