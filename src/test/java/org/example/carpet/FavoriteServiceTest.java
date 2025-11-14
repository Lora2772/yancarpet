package org.example.carpet;

import org.example.carpet.model.Favorite;
import org.example.carpet.repository.mongo.FavoriteRepository;
import org.example.carpet.service.FavoriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository repo;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void add_newItem_shouldAddSuccessfully() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        when(repo.existsByUserEmailAndSku(email, sku)).thenReturn(false);
        when(repo.save(any(Favorite.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        favoriteService.add(email, sku);

        // Assert
        verify(repo).existsByUserEmailAndSku(email, sku);
        verify(repo).save(any(Favorite.class));
    }

    @Test
    void add_existingItem_shouldNotAddAgain() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        when(repo.existsByUserEmailAndSku(email, sku)).thenReturn(true);

        // Act
        favoriteService.add(email, sku);

        // Assert
        verify(repo).existsByUserEmailAndSku(email, sku);
        verify(repo, never()).save(any());
    }

    @Test
    void remove_shouldCallDeleteByEmailAndSku() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        // Act
        favoriteService.remove(email, sku);

        // Assert
        verify(repo).deleteByUserEmailAndSku(email, sku);
    }

    @Test
    void list_shouldReturnAllUserFavorites() {
        // Arrange
        String email = "user@example.com";

        List<Favorite> favorites = Arrays.asList(
            Favorite.builder().userEmail(email).sku("SKU-001").createdAtTs(1000L).build(),
            Favorite.builder().userEmail(email).sku("SKU-002").createdAtTs(2000L).build(),
            Favorite.builder().userEmail(email).sku("SKU-003").createdAtTs(3000L).build()
        );

        when(repo.findByUserEmail(email)).thenReturn(favorites);

        // Act
        List<Favorite> result = favoriteService.list(email);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("SKU-001", result.get(0).getSku());
        assertEquals("SKU-002", result.get(1).getSku());
        assertEquals("SKU-003", result.get(2).getSku());
        verify(repo).findByUserEmail(email);
    }

    @Test
    void list_emptyList_shouldReturnEmptyList() {
        // Arrange
        String email = "user@example.com";
        when(repo.findByUserEmail(email)).thenReturn(List.of());

        // Act
        List<Favorite> result = favoriteService.list(email);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toggle_notExisting_shouldAddAndReturnTrue() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        when(repo.existsByUserEmailAndSku(email, sku)).thenReturn(false);
        when(repo.save(any(Favorite.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        boolean result = favoriteService.toggle(email, sku);

        // Assert
        assertTrue(result); // true means added
        verify(repo).save(any(Favorite.class));
        verify(repo, never()).deleteByUserEmailAndSku(anyString(), anyString());
    }

    @Test
    void toggle_existing_shouldRemoveAndReturnFalse() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        when(repo.existsByUserEmailAndSku(email, sku)).thenReturn(true);

        // Act
        boolean result = favoriteService.toggle(email, sku);

        // Assert
        assertFalse(result); // false means removed
        verify(repo).deleteByUserEmailAndSku(email, sku);
        verify(repo, never()).save(any());
    }

    @Test
    void has_existingItem_shouldReturnTrue() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        when(repo.existsByUserEmailAndSku(email, sku)).thenReturn(true);

        // Act
        boolean result = favoriteService.has(email, sku);

        // Assert
        assertTrue(result);
        verify(repo).existsByUserEmailAndSku(email, sku);
    }

    @Test
    void has_nonExistingItem_shouldReturnFalse() {
        // Arrange
        String email = "user@example.com";
        String sku = "SKU-001";

        when(repo.existsByUserEmailAndSku(email, sku)).thenReturn(false);

        // Act
        boolean result = favoriteService.has(email, sku);

        // Assert
        assertFalse(result);
        verify(repo).existsByUserEmailAndSku(email, sku);
    }
}
