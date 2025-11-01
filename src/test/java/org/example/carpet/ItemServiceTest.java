package org.example.carpet.service;

import org.example.carpet.model.ItemDocument;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for catalog / search behavior.
 * Verifies that:
 *  - we can upsert a SKU
 *  - we can filter by keyword, category, color, roomType
 *
 * upsertItem() 可以保存/更新 SKU
 * search() 能按关键词过滤商品（比如 "wool carpet", "hotel carpet", "living room carpet"...）
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    ItemDocumentRepository itemRepository;   // ✅ 使用 Mongo 版仓库

    @InjectMocks
    ItemService itemService;

    @Test
    void upsertItem_shouldCreateOrUpdateBySku() {
        ItemDocument req = ItemDocument.builder()
                .sku("RUG-12345")
                .name("Persian Wool Carpet")
                .category("wool carpet")
                .description("Soft red handmade carpet for living room")
                .build();

        // simulate DB: no existing SKU yet
        when(itemRepository.findBySku("RUG-12345")).thenReturn(Optional.empty());

        // simulate save (回传入参作为保存结果)
        when(itemRepository.save(any(ItemDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemDocument saved = itemService.upsertItem(req);

        assertEquals("RUG-12345", saved.getSku());
        assertEquals("Persian Wool Carpet", saved.getName());
        verify(itemRepository).save(any(ItemDocument.class));
    }

    @Test
    void search_shouldMatchByKeywordColorAndRoomType() {
        ItemDocument livingRoomRed = ItemDocument.builder()
                .sku("RUG-RED")
                .name("Red Wool Carpet")
                .category("wool carpet")
                .description("Luxury wool carpet for living room")
                .color("red")
                .roomType(List.of("living room", "home"))
                .build();

        ItemDocument hotelTile = ItemDocument.builder()
                .sku("HOTEL-TILE")
                .name("Commercial Carpet Tile")
                .category("carpet tiles")
                .description("Durable, fire resistant hotel carpet tile")
                .color("blue")
                .roomType(List.of("hotel", "office"))
                .build();

        when(itemRepository.findAll()).thenReturn(List.of(livingRoomRed, hotelTile));

        // search for living room + red
        List<ItemDocument> result = itemService.search(
                "wool carpet",   // keyword
                "wool carpet",   // category
                "red",           // color
                "living room"    // roomType
        );

        assertEquals(1, result.size());
        assertEquals("RUG-RED", result.get(0).getSku());
    }
}
