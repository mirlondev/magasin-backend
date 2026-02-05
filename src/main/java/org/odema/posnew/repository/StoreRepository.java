package org.odema.posnew.repository;

import org.odema.posnew.entity.Store;
import org.odema.posnew.entity.enums.StoreType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    List<Store> findAllByIsActiveTrue();

    List<Store> findByStoreTypeAndIsActiveTrue(StoreType storeType);

    Optional<Store> findByStoreIdAndIsActiveTrue(UUID storeId);

    List<Store> findByStatus(String status);

    boolean existsByNameAndIsActiveTrue(String name);
}