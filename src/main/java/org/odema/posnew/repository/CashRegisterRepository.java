package org.odema.posnew.repository;

import org.odema.posnew.entity.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, UUID> {

    Optional<CashRegister> findByRegisterNumber(String registerNumber);

    List<CashRegister> findByStore_StoreId(UUID storeId);

    List<CashRegister> findByStore_StoreIdAndIsActiveTrue(UUID storeId);

    boolean existsByRegisterNumber(String registerNumber);
}
