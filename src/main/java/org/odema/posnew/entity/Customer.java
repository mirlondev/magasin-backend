package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customers")
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

    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;

    @Column(name = "total_purchases", precision = 12)
    private Double totalPurchases = 0.0;

    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // Méthodes utilitaires
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addLoyaltyPoints(Integer points) {
        this.loyaltyPoints = (this.loyaltyPoints == null ? 0 : this.loyaltyPoints) + points;
    }

    public void removeLoyaltyPoints(Integer points) {
        this.loyaltyPoints = Math.max(0, (this.loyaltyPoints == null ? 0 : this.loyaltyPoints) - points);
    }

    public void addPurchase(Double amount) {
        this.totalPurchases = (this.totalPurchases == null ? 0.0 : this.totalPurchases) + amount;
        this.lastPurchaseDate = LocalDateTime.now();
    }
}
