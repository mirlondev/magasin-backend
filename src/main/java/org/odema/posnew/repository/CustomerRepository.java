package org.odema.posnew.repository;

import org.odema.posnew.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%')")
    List<Customer> searchCustomers(@Param("keyword") String keyword);

    List<Customer> findByIsActiveTrue();

    @Query("SELECT c FROM Customer c WHERE c.isActive = true ORDER BY c.totalPurchases DESC")
    List<Customer> findTopCustomers();

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true")
    Long countActiveCustomers();

    @Query("SELECT SUM(c.totalPurchases) FROM Customer c WHERE c.isActive = true")
    Double getTotalPurchases();
}
