package com.trimly.repository;

import com.trimly.entity.Shop;
import com.trimly.enums.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findBySlug(String slug);
    Optional<Shop> findByOwner_Id(Long ownerId);

    boolean existsBySlug(String slug);
    long countByStatus(ShopStatus status);

    // ── Public browsing queries ───────────────────────────────────────────

    /** All active shops optionally filtered by city and/or area */
    @Query("""
        SELECT s FROM Shop s
        WHERE s.status = 'ACTIVE'
          AND (:city IS NULL OR LOWER(s.city) = LOWER(:city))
          AND (:area IS NULL OR LOWER(s.area) = LOWER(:area))
          AND (:q IS NULL OR LOWER(s.shopName) LIKE LOWER(CONCAT('%',:q,'%'))
               OR LOWER(s.location) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY s.avgRating DESC
        """)
    List<Shop> searchActive(
        @Param("q")    String q,
        @Param("city") String city,
        @Param("area") String area
    );

    /** Distinct cities that have at least one active shop */
    @Query("SELECT DISTINCT s.city FROM Shop s WHERE s.status = 'ACTIVE' AND s.city IS NOT NULL ORDER BY s.city")
    List<String> findActiveCities();

    /** Areas within a city that have at least one active shop */
    @Query("SELECT DISTINCT s.area FROM Shop s WHERE s.status = 'ACTIVE' AND s.city = :city AND s.area IS NOT NULL ORDER BY s.area")
    List<String> findActiveAreasInCity(@Param("city") String city);

    List<Shop> findAllByOrderByCreatedAtDesc();
}
