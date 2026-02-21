package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_transactions", indexes = {
        @Index(name = "idx_loyalty_customer", columnList = "customer_id,transaction_date"),
        @Index(name = "idx_loyalty_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Integer pointsChange;

    @Column(nullable = false)
    private Integer newBalance;

    @Column(length = 255)
    private String reason;

    @Column(name = "order_id")
    private UUID orderId;

    @CreationTimestamp
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    @Column(name = "created_by")
    private UUID createdBy;
}
