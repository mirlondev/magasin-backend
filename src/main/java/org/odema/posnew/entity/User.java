package org.odema.posnew.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.StoreType;
import org.odema.posnew.entity.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
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

    @Column(nullable = true)
    private String address;

    @Column(nullable = false)
    private Boolean active = true;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private  UserRole userRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @UpdateTimestamp
    @Column(name = "last_login")
    private  LocalDateTime lastLogin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_store_id")
    private Store assignedStore;
    // Ajouter cette méthode pour vérifier les permissions
    public boolean canManageStore(Store store) {
        if (this.userRole == UserRole.ADMIN) return true;

        if (store == null || this.assignedStore == null) return false;

        return switch (this.userRole) {
            case DEPOT_MANAGER -> store.getStoreType().equals(StoreType.WAREHOUSE) &&
                    store.getStoreId().equals(this.assignedStore.getStoreId());
            case STORE_ADMIN -> store.getStoreType().equals(StoreType.SHOP) &&
                    store.getStoreId().equals(this.assignedStore.getStoreId());
            default -> false;
        };
    }

    public String getFullName() {
        return  username;

    }
}