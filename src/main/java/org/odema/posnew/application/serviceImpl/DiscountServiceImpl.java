package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.request.ApplyDiscountRequest;
import org.odema.posnew.application.dto.request.DiscountResult;
import org.odema.posnew.domain.model.StoreProductPrice;
import org.odema.posnew.domain.repository.CustomerRepository;
import org.odema.posnew.domain.repository.StoreProductPriceRepository;
import org.odema.posnew.domain.service.DiscountService;
import org.odema.posnew.domain.service.LoyaltyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final StoreProductPriceRepository priceRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyService loyaltyService;


    @Override
    public DiscountResult applyDiscount(ApplyDiscountRequest request) {
        // Logique d'application de remise manuelle ou promotionnelle
        // Cette méthode peut étendre les remises calculées automatiquement
        return null;
    }

    @Override
    public BigDecimal calculateGlobalDiscount(UUID orderId, BigDecimal subtotal, UUID customerId) {
        // Remise globale sur commande (ex: remise panier, codes promo)
        return BigDecimal.ZERO;
    }

    @Override
    public void validateDiscountRules(UUID storeId) {
        // Validation des règles de remise du magasin
    }

    private String buildDiscountSource(StoreProductPrice price, BigDecimal loyaltyDiscount) {
        if (loyaltyDiscount.compareTo(BigDecimal.ZERO) > 0) {
            return "COMBINED";
        }
        return price.hasActiveDiscount() ? "PROMOTION" : "NONE";
    }

    private String buildDiscountReason(StoreProductPrice price, UUID customerId) {
        StringBuilder reason = new StringBuilder();
        if (price.hasActiveDiscount()) {
            reason.append("Promotion en cours");
        }
        if (customerId != null) {
            if (reason.length() > 0) reason.append(" + ");
            reason.append("Remise fidélité");
        }
        return reason.toString();
    }


    @Override
    public DiscountResult calculateItemDiscount(UUID productId, UUID storeId,
                                                Integer quantity, UUID customerId) {
        StoreProductPrice price = (StoreProductPrice) priceRepository
                .findActivePriceForProductAndStore(productId, storeId)
                .orElseThrow(() -> new NotFoundException("Prix non trouvé"));

        BigDecimal unitPrice  = price.getBasePrice();
        BigDecimal totalBase  = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Remise produit (sur le total HT)
        BigDecimal productDiscount = price.getDiscountValue()
                .multiply(BigDecimal.valueOf(quantity));

        // Remise fidélité sur le net HT après remise produit
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        if (customerId != null) {
            BigDecimal netHT = totalBase.subtract(productDiscount).max(BigDecimal.ZERO);
            loyaltyDiscount = loyaltyService.calculateTierDiscount(customerId, netHT);
        }

        BigDecimal totalDiscount = productDiscount.add(loyaltyDiscount);

        // ✅ Net HT après toutes les remises
        BigDecimal netAfterDiscount = totalBase.subtract(totalDiscount).max(BigDecimal.ZERO);

        // ✅ Appliquer la taxe sur le net remisé (ordre correct : remise AVANT taxe)
        BigDecimal taxAmount = BigDecimal.ZERO;
        if (price.getTaxRate() != null
                && price.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = netAfterDiscount
                    .multiply(price.getTaxRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // ✅ Prix TTC final cohérent avec OrderItem.calculate()
        BigDecimal finalPriceTTC = netAfterDiscount.add(taxAmount);

        return new DiscountResult(
                totalBase,
                totalDiscount,
                price.getDiscountPercentage(),
                price.getDiscountAmount(),
                finalPriceTTC,
                buildDiscountSource(price, loyaltyDiscount),
                buildDiscountReason(price, customerId),
                0,
                0
        );
    }
}
