package org.example.carpet.service;

import lombok.RequiredArgsConstructor;
import org.example.carpet.model.Favorite;
import org.example.carpet.repository.mongo.FavoriteRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository repo;

    public List<Favorite> list(String email) {
        return repo.findByUserEmail(email);
    }

    public void add(String email, String sku) {
        if (repo.existsByUserEmailAndSku(email, sku)) return;
        repo.save(Favorite.builder()
                .userEmail(email)
                .sku(sku)
                .createdAtTs(Instant.now().toEpochMilli())
                .build());
    }

    public void remove(String email, String sku) {
        repo.deleteByUserEmailAndSku(email, sku);
    }

    /** 返回是否已收藏（方便前端初始化心形状态） */
    public boolean has(String email, String sku) {
        return repo.existsByUserEmailAndSku(email, sku);
    }

    /** 切换收藏状态，返回当前是否收藏 */
    public boolean toggle(String email, String sku) {
        if (has(email, sku)) {
            remove(email, sku);
            return false;
        } else {
            add(email, sku);
            return true;
        }
    }
}
