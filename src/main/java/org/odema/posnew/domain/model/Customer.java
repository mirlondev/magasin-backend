package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.LoyaltyTier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_email", columnList = "email", unique = true),
        @Index(name = "idx_customer_phone", columnList = "phone", unique = true),
        @Index(name = "idx_customer_loyalty", columnList = "loyalty_points"),
        @Index(name = "idx_customer_tier", columnList = "loyalty_tier"),
        @Index(name = "idx_customer_active", columnList = "is_active")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customer_id", updatable = false, nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    // Système de fidélité
    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "loyalty_tier", length = 20)
    @Builder.Default
    private LoyaltyTier loyaltyTier = LoyaltyTier.BRONZE;

    @Column(name = "total_purchases", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "purchase_count")
    @Builder.Default
    private Integer purchaseCount = 0;

    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LoyaltyTransaction> loyaltyHistory = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    // Méthodes métier
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addLoyaltyPoints(int points, String reason, UUID orderId) {
        this.loyaltyPoints += points;

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(this)
                .pointsChange(points)
                .newBalance(this.loyaltyPoints)
                .reason(reason)
                .orderId(orderId)
                .transactionDate(LocalDateTime.now())
                .build();

        loyaltyHistory.add(transaction);
        updateTier();
    }

    public boolean useLoyaltyPoints(int points, String reason, UUID orderId) {
        if (points > this.loyaltyPoints) {
            return false;
        }

        this.loyaltyPoints -= points;

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(this)
                .pointsChange(-points)
                .newBalance(this.loyaltyPoints)
                .reason(reason)
                .orderId(orderId)
                .transactionDate(LocalDateTime.now())
                .build();

        loyaltyHistory.add(transaction);
        return true;
    }

    public BigDecimal convertPointsToDiscount(int points) {
        // 100 points = 1 unité monétaire
        return BigDecimal.valueOf(points / 100.0).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void updateTier() {
        this.loyaltyTier = LoyaltyTier.fromAmount(this.totalPurchases);
    }


    public BigDecimal getTierDiscountRate() {
        return loyaltyTier.getDiscountRate();
    }

    public int getAvailablePoints() {
        return loyaltyPoints;
    }
}
