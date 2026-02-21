package org.odema.posnew.domain.repository;

import org.odema.posnew.domain.model.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {

    List<LoyaltyTransaction> findByCustomer_CustomerId(UUID customerId);

    List<LoyaltyTransaction> findByCustomer_CustomerIdOrderByTransactionDateDesc(UUID customerId);

    List<LoyaltyTransaction> findByOrderId(UUID orderId);
}
