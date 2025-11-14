package org.example.carpet;

import org.example.carpet.cassandra.entities.CartItem;
import org.example.carpet.cassandra.keys.CartItemKey;
import org.example.carpet.cassandra.repos.CartItemRepo;
import org.example.carpet.dto.CartChangeQtyRequest;
import org.example.carpet.dto.CartItemView;
import org.example.carpet.dto.CartUpsertRequest;
import org.example.carpet.model.ItemDocument;
import org.example.carpet.repository.mongo.ItemDocumentRepository;
import org.example.carpet.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.cql.CqlOperations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepo cartItemRepo;

    @Mock
    private ItemDocumentRepository itemRepo;

    @Mock
    private CassandraTemplate cassandraTemplate;

    @InjectMocks
    private CartService cartService;

    @Test
    void list_shouldReturnAllCartItems() {
        // Arrange
        String email = "user@example.com";

        CartItemKey key1 = new CartItemKey(email, "SKU-001");
        CartItemKey key2 = new CartItemKey(email, "SKU-002");

        CartItem item1 = new CartItem(key1, 2, BigDecimal.valueOf(50.00), 1000L);
        CartItem item2 = new CartItem(key2, 1, BigDecimal.valueOf(100.00), 2000L);

        ItemDocument doc1 = ItemDocument.builder()
                .sku("SKU-001")
                .name("Item 1")
                .imageUrl("url1")
                .roomType(List.of("Living Room"))
                .keywords(List.of("modern"))
                .build();

        ItemDocument doc2 = ItemDocument.builder()
                .sku("SKU-002")
                .name("Item 2")
                .imageUrl("url2")
                .roomType(List.of("Bedroom"))
                .keywords(List.of("classic"))
                .build();

        when(cartItemRepo.findByUserEmail(email)).thenReturn(Arrays.asList(item1, item2));
        when(itemRepo.findBySku("SKU-001")).thenReturn(Optional.of(doc1));
        when(itemRepo.findBySku("SKU-002")).thenReturn(Optional.of(doc2));

        // Act
        List<CartItemView> result = cartService.list(email);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("SKU-001", result.get(0).getSku());
        assertEquals("Item 1", result.get(0).getName());
        assertEquals(2, result.get(0).getQuantity());
        assertEquals("SKU-002", result.get(1).getSku());
    }

    @Test
    void list_emptyCart_shouldReturnEmptyList() {
        // Arrange
        String email = "user@example.com";
        when(cartItemRepo.findByUserEmail(email)).thenReturn(List.of());

        // Act
        List<CartItemView> result = cartService.list(email);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void upsert_newItem_shouldAddToCart() {
        // Arrange
        String email = "user@example.com";
        CartUpsertRequest request = new CartUpsertRequest();
        request.setSku("SKU-001");
        request.setQuantity(2);
        request.setPrice(BigDecimal.valueOf(99.99));

        CartItemKey key = new CartItemKey(email, "SKU-001");

        when(cartItemRepo.findById(key)).thenReturn(Optional.empty());

        CqlOperations cqlOps = mock(CqlOperations.class);
        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOps);

        // Act
        cartService.upsert(email, request);

        // Assert
        verify(cqlOps).execute(anyString(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void upsert_existingItem_shouldUpdate() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        CartItemKey key = new CartItemKey(email, sku);
        CartItem existing = new CartItem(key, 1, BigDecimal.valueOf(50.00), 1000L);

        CartUpsertRequest request = new CartUpsertRequest();
        request.setSku(sku);
        request.setQuantity(5);
        request.setPrice(BigDecimal.valueOf(99.99));

        when(cartItemRepo.findById(key)).thenReturn(Optional.of(existing));

        CqlOperations cqlOps = mock(CqlOperations.class);
        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOps);

        // Act
        cartService.upsert(email, request);

        // Assert
        verify(cqlOps).execute(anyString(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void changeQty_shouldUpdateQuantity() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        CartItemKey key = new CartItemKey(email, sku);
        CartItem existing = new CartItem(key, 1, BigDecimal.valueOf(50.00), 1000L);

        CartChangeQtyRequest request = new CartChangeQtyRequest();
        request.setSku(sku);
        request.setQuantity(10);

        when(cartItemRepo.findById(key)).thenReturn(Optional.of(existing));

        CqlOperations cqlOps = mock(CqlOperations.class);
        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOps);

        // Act
        cartService.changeQty(email, request);

        // Assert
        verify(cqlOps).execute(anyString(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void changeQty_zeroQuantity_shouldDelete() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        CartChangeQtyRequest request = new CartChangeQtyRequest();
        request.setSku(sku);
        request.setQuantity(0);

        CartItemKey key = new CartItemKey(email, sku);

        // Act
        cartService.changeQty(email, request);

        // Assert
        verify(cartItemRepo).deleteById(key);
        verify(cassandraTemplate, never()).getCqlOperations();
    }

    @Test
    void remove_shouldDeleteItem() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        // Act
        cartService.remove(email, sku);

        // Assert
        verify(cartItemRepo).deleteById(any(CartItemKey.class));
    }

    @Test
    void clear_shouldDeleteAllUserItems() {
        // Arrange
        String email = "user@example.com";

        CartItemKey key1 = new CartItemKey(email, "SKU-001");
        CartItemKey key2 = new CartItemKey(email, "SKU-002");

        CartItem item1 = new CartItem(key1, 2, BigDecimal.valueOf(50.00), 1000L);
        CartItem item2 = new CartItem(key2, 1, BigDecimal.valueOf(100.00), 2000L);

        when(cartItemRepo.findByUserEmail(email)).thenReturn(Arrays.asList(item1, item2));

        // Act
        cartService.clear(email);

        // Assert
        verify(cartItemRepo, times(2)).deleteById(any(CartItemKey.class));
    }

    @Test
    void upsert_withNullPrice_shouldFetchFromItemRepo() {
        // Arrange
        String email = "user@example.com";
        CartUpsertRequest request = new CartUpsertRequest();
        request.setSku("SKU-001");
        request.setQuantity(2);
        request.setPrice(null);

        ItemDocument doc = ItemDocument.builder()
                .sku("SKU-001")
                .unitPrice(BigDecimal.valueOf(149.99))
                .build();

        when(itemRepo.findBySku("SKU-001")).thenReturn(Optional.of(doc));
        when(cartItemRepo.findById(any())).thenReturn(Optional.empty());

        CqlOperations cqlOps = mock(CqlOperations.class);
        when(cassandraTemplate.getCqlOperations()).thenReturn(cqlOps);

        // Act
        cartService.upsert(email, request);

        // Assert
        verify(itemRepo).findBySku("SKU-001");
        verify(cqlOps).execute(anyString(), any(), any(), any(), any(), any(), anyInt());
    }
}
