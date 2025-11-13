package org.example.carpet.service;

import org.example.carpet.model.ItemDocument;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for inventory behavior:
 *  - availableQuantity lookup
 *  - reserve / release
 *  - delivery promise "approximately 15 business days"
 *
 *checkInventory() 返回可售数量 + 15 个工作日交付承诺
 * reserve() 成功时扣库存，失败时不扣
 * release() 会把库存加回去
 *
 *  这一组 test 把"海运 15 个工作日交付"的承诺写死在断言里
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    ItemDocumentRepository itemRepository;

    @Mock
    CassandraTemplate cassandraTemplate;

    @InjectMocks
    InventoryService inventoryService;

    @Test
    void checkInventory_shouldReturnQuantityAnd15BusinessDays() {
        ItemDocument item = ItemDocument.builder()
                .sku("RUG-12345")
                .stockQuantity(18)
                .build();
        when(itemRepository.findBySku("RUG-12345"))
                .thenReturn(Optional.of(item));

        InventoryService.InventoryStatus status =
                inventoryService.checkInventory("RUG-12345");

        assertEquals("RUG-12345", status.getSku());
        assertEquals(18, status.getAvailableQuantity());
        assertEquals(15, status.getEstimatedDeliveryBusinessDays());
        assertEquals("approximately 15 business days", status.getEstimatedDeliveryText());
        assertTrue(status.getNotes().toLowerCase().contains("sea freight"));
    }

    @Test
    void reserve_shouldReturnTrueWhenStockEnough() {
        when(itemRepository.tryDeduct("RUG-RED", 2))
                .thenReturn(1); // 成功返回 1

        boolean ok = inventoryService.reserve("RUG-RED", 2);
        assertTrue(ok);
    }

    @Test
    void reserve_shouldReturnFalseWhenStockLow() {
        when(itemRepository.tryDeduct("RUG-RED", 99))
                .thenReturn(0); // 失败返回 0

        boolean ok = inventoryService.reserve("RUG-RED", 99);
        assertFalse(ok);
    }

    @Test
    void release_shouldCallRepositoryAndReturnTrue() {
        when(itemRepository.tryRestock("RUG-RED", 2))
                .thenReturn(1); // 成功返回 1

        boolean ok = inventoryService.release("RUG-RED", 2);
        assertTrue(ok);
        verify(itemRepository).tryRestock("RUG-RED", 2);
    }
}
