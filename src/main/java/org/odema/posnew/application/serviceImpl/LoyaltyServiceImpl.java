package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.api.exception.BusinessException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.request.DiscountResult;
import org.odema.posnew.application.dto.response.LoyaltySummaryResponse;
import org.odema.posnew.application.dto.response.LoyaltyTransactionResponse;
import org.odema.posnew.domain.model.Customer;
import org.odema.posnew.domain.model.LoyaltyTransaction;
import org.odema.posnew.domain.model.enums.LoyaltyTier;
import org.odema.posnew.domain.repository.CustomerRepository;
import org.odema.posnew.domain.repository.LoyaltyTransactionRepository;
import org.odema.posnew.domain.service.LoyaltyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository transactionRepository;

    private static final int POINTS_PER_CURRENCY_UNIT = 1; // 1 point par unité monétaire
    private static final int POINTS_TO_CURRENCY_RATE = 100; // 100 points = 1 unité

    @Override
    @Transactional(readOnly = true)
    public LoyaltySummaryResponse getCustomerLoyalty(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        int pointsToNext = calculatePointsToNextTier(customer);

        return new LoyaltySummaryResponse(
                customerId,
                customer.getFullName(),
                customer.getLoyaltyPoints(),
                customer.getLoyaltyTier(),
                customer.getTierDiscountRate(),
                pointsToNext,
                customer.getTotalPurchases(),
                customer.getPurchaseCount(),
                convertPointsToCurrency(customer.getLoyaltyPoints())
        );
    }

    @Override
    @Transactional
    public DiscountResult usePointsForDiscount(UUID customerId, int points, UUID orderId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (points > customer.getLoyaltyPoints()) {
            throw new BusinessException("Points insuffisants. Disponible: " +
                    customer.getLoyaltyPoints());
        }

        BigDecimal discountValue = convertPointsToCurrency(points);

        // Créer transaction de débit
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .pointsChange(-points)
                .newBalance(customer.getLoyaltyPoints() - points)
                .reason("Utilisation remise commande " + orderId)
                .orderId(orderId)
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        // Mettre à jour client
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - points);
        customerRepository.save(customer);

        log.info("Customer {} used {} points for {} discount",
                customerId, points, discountValue);

        return new DiscountResult(
                BigDecimal.ZERO, // originalPrice pas applicable ici
                discountValue,
                BigDecimal.ZERO,
                discountValue,
                BigDecimal.ZERO.subtract(discountValue),
                "LOYALTY",
                "Remise fidélité " + points + " points",
                points,
                0
        );
    }

    @Override
    @Transactional
    public void awardPointsForPurchase(UUID customerId, BigDecimal amount, UUID orderId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        int basePoints = amount.intValue() * POINTS_PER_CURRENCY_UNIT;
        int bonusPoints = calculateTierBonus(basePoints, customer.getLoyaltyTier());
        int totalPoints = basePoints + bonusPoints;

      //  customer.addLoyaltyPoints(totalPoints);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .pointsChange(totalPoints)
                .newBalance(customer.getLoyaltyPoints())
                .reason("Achat commande " + orderId)
                .orderId(orderId)
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        customerRepository.save(customer);

        log.info("Awarded {} points (base: {}, bonus: {}) to customer {}",
                totalPoints, basePoints, bonusPoints, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTierDiscount(UUID customerId, BigDecimal amount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        return amount.multiply(customer.getTierDiscountRate())
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int calculatePointsEarned(BigDecimal amount, LoyaltyTier tier) {
        int base = amount.intValue();
        return base + calculateTierBonus(base, tier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoyaltyTransactionResponse> getTransactionHistory(UUID customerId) {
        return transactionRepository.findByCustomer_CustomerIdOrderByTransactionDateDesc(customerId)
                .stream()
                .map(this::mapToTransactionResponse)
                .toList();
    }

    @Override
    @Transactional
    public void adjustPointsManually(UUID customerId, int delta, String reason, UUID adminId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        int newBalance = customer.getLoyaltyPoints() + delta;
        if (newBalance < 0) {
            throw new BusinessException("Le solde ne peut pas être négatif");
        }

        customer.setLoyaltyPoints(newBalance);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .pointsChange(delta)
                .newBalance(newBalance)
                .reason("Ajustement manuel: " + reason + " (par admin: " + adminId + ")")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        customerRepository.save(customer);
    }

    // Méthodes utilitaires
    private BigDecimal convertPointsToCurrency(int points) {
        return BigDecimal.valueOf(points / (double) POINTS_TO_CURRENCY_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int calculateTierBonus(int basePoints, LoyaltyTier tier) {
        return switch (tier) {
            case BRONZE -> 0;
            case SILVER -> (int) (basePoints * 0.1); // +10%
            case GOLD -> (int) (basePoints * 0.25);  // +25%
            case PLATINUM -> (int) (basePoints * 0.5); // +50%
        };
    }

    private int calculatePointsToNextTier(Customer customer) {
        return customerRepository.findByLoyaltyTier(String.valueOf(customer.getLoyaltyTier()))
                .stream()
                .mapToInt(Customer::getLoyaltyPoints)
                .filter(points -> points > customer.getLoyaltyPoints())
                .min()
                .orElse(0) - customer.getLoyaltyPoints();
    }

    private LoyaltyTransactionResponse mapToTransactionResponse(LoyaltyTransaction tx) {
        return new LoyaltyTransactionResponse(
                tx.getTransactionId(),
                tx.getPointsChange(),
                tx.getNewBalance(),
                tx.getReason(),
                tx.getOrderId(),
                tx.getTransactionDate()
        );
    }
}
