package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.api.exception.BusinessException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.request.*;
import org.odema.posnew.application.dto.response.StoreProductPriceResponse;
import org.odema.posnew.application.mapper.StoreProductPriceMapper;
import org.odema.posnew.domain.model.Product;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.StoreProductPrice;
import org.odema.posnew.domain.repository.ProductRepository;
import org.odema.posnew.domain.repository.StoreProductPriceRepository;
import org.odema.posnew.domain.repository.StoreRepository;
import org.odema.posnew.domain.service.StorePricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorePricingServiceImpl implements StorePricingService {

    private final StoreProductPriceRepository priceRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StoreProductPriceMapper priceMapper;

    @Override
    @Transactional
    public StoreProductPriceResponse setProductPrice(StoreProductPriceRequest request) {
        log.info("Setting price for product {} in store {}",
                request.productId(), request.storeId());

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Magasin non trouvé"));

        // Désactiver l'ancien prix actif s'il existe
        priceRepository.findActivePriceForProductAndStore(request.productId(), request.storeId())
                .ifPresent(oldPrice -> {
                    oldPrice.setIsActive(false);
                    priceRepository.save(oldPrice);
                    log.info("Ancien prix désactivé: {}", oldPrice.getPriceId());
                });

        LocalDateTime effectiveDate = request.effectiveDate() != null
                ? request.effectiveDate()
                : LocalDateTime.now();

        StoreProductPrice price = StoreProductPrice.builder()
                .product(product)
                .store(store)
                .basePrice(request.newBasePrice())
                .taxRate(request.taxRate() != null ? request.taxRate() : new BigDecimal("19.25"))
                .discountPercentage(request.discountPercentage() != null ? request.discountPercentage() : BigDecimal.ZERO)
                .discountAmount(request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO)
                .effectiveDate(effectiveDate)
                .endDate(request.endDate())
                .isActive(true)
                .description(request.reason())
                .build();

        StoreProductPrice saved = priceRepository.save(price);
        log.info("Price set: {} -> {} (final: {})",
                request.newBasePrice(), saved.getPriceId(), saved.getFinalPrice());

        return priceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StoreProductPriceResponse applyTemporaryDiscount(TemporaryDiscountRequest request) {
        log.info("Applying temporary discount {}% to product {} in store {}",
                request.discountPercentage(), request.productId(), request.storeId());

        List<StoreProductPrice> overlapping = priceRepository
                .findOverlappingPrices(request.productId(), request.storeId(),
                        request.startDate(), request.endDate());

        if (!overlapping.isEmpty()) {
            throw new BusinessException("Une remise existe déjà sur cette période");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Magasin non trouvé"));

        StoreProductPrice discountPrice = StoreProductPrice.builder()
                .product(product)
                .store(store)
                .basePrice(getCurrentBasePrice(product.getProductId(), store.getStoreId()))
                .taxRate(getCurrentTaxRate(product.getProductId(), store.getStoreId()))
                .discountPercentage(request.discountPercentage())
                .discountAmount(request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO)
                .effectiveDate(request.startDate())
                .endDate(request.endDate())
                .isActive(true)
                .description("Remise temporaire")
                .build();

        StoreProductPrice saved = priceRepository.save(discountPrice);
        return priceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPrice(UUID productId, UUID storeId, UUID customerId) {
        StoreProductPrice price = getActivePrice(productId, storeId);
        return price.getFinalPrice();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreProductPriceResponse> getActivePricesForStore(UUID storeId) {
        return priceRepository.findActivePricesForStore(storeId).stream()
                .map(priceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreProductPriceResponse> getPriceHistory(UUID productId, UUID storeId) {
        return priceRepository.findPriceHistory(productId, storeId).stream()
                .map(priceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void bulkUpdatePrices(BulkPriceUpdateRequest request) {
        log.info("Bulk price update for {} products in store {}",
                request.productIds().size(), request.storeId());

        for (UUID productId : request.productIds()) {
            // ✅ CORRECTION: Utilise StoreProductPriceRequest (cohérent avec setProductPrice)
            StoreProductPriceRequest singleRequest = new StoreProductPriceRequest(
                    productId,
                    request.storeId(),
                    request.newBasePrice(),
                    request.taxRate(),
                    request.discountPercentage(),
                    request.discountAmount(),
                    request.effectiveDate(),
                    request.endDate(),
                    "Bulk update"
            );
            setProductPrice(singleRequest);
        }
    }

    @Override
    @Transactional
    public StoreProductPriceResponse updatePrice(UUID priceId, UpdatePriceRequest request) {
        StoreProductPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> new NotFoundException("Prix non trouvé"));

        if (request.basePrice() != null) price.setBasePrice(request.basePrice());
        if (request.taxRate() != null) price.setTaxRate(request.taxRate());
        if (request.discountPercentage() != null) price.setDiscountPercentage(request.discountPercentage());
        if (request.discountAmount() != null) price.setDiscountAmount(request.discountAmount());
        if (request.endDate() != null) price.setEndDate(request.endDate());
        if (request.description() != null) price.setDescription(request.description());

        StoreProductPrice updated = priceRepository.save(price);
        return priceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deactivatePrice(UUID priceId) {
        StoreProductPrice price = priceRepository.findById(priceId)
                .orElseThrow(() -> new NotFoundException("Prix non trouvé"));
        price.setIsActive(false);
        priceRepository.save(price);
        log.info("Prix désactivé: {}", priceId);
    }

    @Override
    @Transactional
    public StoreProductPriceResponse schedulePriceChange(SchedulePriceRequest request) {
        // ✅ CORRECTION: Utilise StoreProductPriceRequest au lieu de SetProductPriceRequest
        return setProductPrice(new StoreProductPriceRequest(
                request.productId(),
                request.storeId(),
                request.newBasePrice(),
                request.taxRate(),
                request.discountPercentage(),
                request.discountAmount(),
                request.effectiveDate(),
                request.endDate(),
                "Scheduled price change"
        ));
    }

    // Méthodes utilitaires privées
    private StoreProductPrice getActivePrice(UUID productId, UUID storeId) {
        return priceRepository.findActivePriceForProductAndStore(productId, storeId)
                .orElseThrow(() -> new NotFoundException(
                        "Prix non trouvé pour ce produit dans ce magasin"));
    }

    private BigDecimal getCurrentBasePrice(UUID productId, UUID storeId) {
        return getActivePrice(productId, storeId).getBasePrice();
    }

    private BigDecimal getCurrentTaxRate(UUID productId, UUID storeId) {
        return getActivePrice(productId, storeId).getTaxRate();
    }
}