package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.StoreStatus;
import org.odema.posnew.domain.model.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stores", indexes = {
        @Index(name = "idx_store_name", columnList = "name"),
        @Index(name = "idx_store_type", columnList = "store_type"),
        @Index(name = "idx_store_status", columnList = "status"),
        @Index(name = "idx_store_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id", updatable = false, nullable = false)
    private UUID storeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreType storeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StoreStatus status = StoreStatus.PENDING;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String openingHours; // JSON format

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 8)
    private BigDecimal longitude;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_admin_id")
    private User storeAdmin;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StoreStatus.PENDING;
        }
    }

    public boolean isOperational() {
        return status == StoreStatus.ACTIVE && isActive;
    }

    public boolean canSell() {
        return isOperational() && (storeType == StoreType.SHOP || storeType == StoreType.KIOSK);
    }
}
