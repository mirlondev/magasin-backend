package org.odema.posnew.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<Customer> findByIsActiveTrue();

    Page<Customer> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.isActive = true AND " +
            "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%'))")
    List<Customer> searchCustomers(@Param("keyword") String keyword);

    @Query("SELECT c FROM Customer c WHERE c.isActive = true ORDER BY c.totalPurchases DESC")
    List<Customer> findTopCustomers(Pageable pageable);

    List<Customer> findByLoyaltyTier(String tier);
}
