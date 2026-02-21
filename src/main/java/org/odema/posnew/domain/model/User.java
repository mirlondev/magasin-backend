package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.StoreType;
import org.odema.posnew.domain.model.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_phone", columnList = "phone", unique = true),
        @Index(name = "idx_user_role", columnList = "user_role"),
        @Index(name = "idx_user_store", columnList = "assigned_store_id"),
        @Index(name = "idx_user_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column
    private String address;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "user_role")
    private UserRole userRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_store_id")
    private Store assignedStore;

    // Méthodes métier
    public boolean canManageStore(Store store) {
        if (this.userRole == UserRole.ADMIN) return true;
        if (store == null || this.assignedStore == null) return false;

        return switch (this.userRole) {
            case DEPOT_MANAGER -> store.getStoreType() == StoreType.WAREHOUSE &&
                    store.getStoreId().equals(this.assignedStore.getStoreId());
            case STORE_ADMIN -> store.getStoreType() == StoreType.SHOP &&
                    store.getStoreId().equals(this.assignedStore.getStoreId());
            default -> false;
        };
    }

    public boolean hasPermission(UserRole minimumRole) {
        int myLevel = this.userRole.ordinal();
        int requiredLevel = minimumRole.ordinal();
        return myLevel <= requiredLevel; // ADMIN=0, plus petit = plus privilégié
    }

    public String getFullName() {
        return username; // Peut être étendu avec firstName/lastName séparés
    }

    public boolean isAssignedTo(Store store) {
        return store != null && store.getStoreId().equals(
                this.assignedStore != null ? this.assignedStore.getStoreId() : null
        );
    }
}
