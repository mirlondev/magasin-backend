package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_registers", indexes = {
        @Index(name = "idx_register_number", columnList = "register_number", unique = true),
        @Index(name = "idx_register_store", columnList = "store_id"),
        @Index(name = "idx_register_active", columnList = "is_active")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cash_register_id", updatable = false, nullable = false)
    private UUID cashRegisterId;

    @Column(nullable = false, unique = true, length = 20)
    private String registerNumber; // "Caisse-01", "Caisse-02"

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 200)
    private String location; // "Rez-de-chaussée", "Étage 1"

    @Column(length = 100)
    private String model; // Modèle de l'appareil

    @Column(length = 100)
    private String serialNumber; // Numéro de série

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        return isActive;
    }
}
