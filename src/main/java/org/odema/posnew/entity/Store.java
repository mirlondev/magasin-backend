package org.odema.posnew.entity;
import jakarta.persistence.*;
import lombok.*;
import org.odema.posnew.entity.enums.StoreStatus;
import org.odema.posnew.entity.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stores")
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID storeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    private String city;
    private String postalCode;
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreType storeType; // DEPOT ou SHOP


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status;


    private String phone;
    private String email;

    // Horaires d'ouverture (peut être JSON stocké en texte)
    @Column(columnDefinition = "TEXT")
    private String openingHours;

    // Coordonnées géographiques
    @Column(precision = 10)
    private BigDecimal latitude;

    @Column(precision = 10)
    private BigDecimal longitude;



    // Métadonnées
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean isActive = true;

    // Relation inverse (1 Store a plusieurs employés)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_admin_id", unique = true)
    private User storeAdmin;



    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status=StoreStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}