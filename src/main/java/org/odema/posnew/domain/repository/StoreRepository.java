package org.odema.posnew.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByName(String name);

    List<Store> findByStoreTypeAndIsActiveTrue(StoreType storeType);

    List<Store> findByStatusAndIsActiveTrue(StoreStatus status);

    List<Store> findAllByIsActiveTrue();

    Page<Store> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT s FROM Store s WHERE s.isActive = true AND " +
            "(LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Store> searchStores(@Param("search") String search);

    boolean existsByName(String name);
}
