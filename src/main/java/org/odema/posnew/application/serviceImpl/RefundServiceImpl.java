package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.request.RefundItemRequest;
import org.odema.posnew.application.dto.request.RefundRequest;
import org.odema.posnew.application.dto.response.RefundResponse;
import org.odema.posnew.application.mapper.RefundMapper;
import org.odema.posnew.design.context.DocumentBuildContext;
import org.odema.posnew.design.factory.DocumentBuilderFactory;
import org.odema.posnew.domain.model.*;
import org.odema.posnew.domain.model.enums.*;
import org.odema.posnew.domain.repository.*;
import org.odema.posnew.domain.service.DocumentNumberService;
import org.odema.posnew.domain.service.FileStorageService;
import org.odema.posnew.domain.service.RefundService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final TransactionRepository transactionRepository;
    private final DocumentNumberService documentNumberService;
    private final FileStorageService fileStorageService;
    private final ReceiptRepository receiptRepository;
    private final RefundMapper refundMapper;
    private final DocumentBuilderFactory builderFactory;
    private final InventoryRepository inventoryRepository;

    @Value("${app.file.directories.refunds:refunds}")
    private String refundsDirectory;

    @Override
    @Transactional
    public RefundResponse createRefund(RefundRequest request, UUID cashierId) {
        log.info("Création remboursement pour commande {} par caissier {}",
                request.orderId(), cashierId);

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de rembourser une commande annulée");
        }

        ShiftReport shift = shiftReportRepository.findOpenShiftByCashier(cashierId)
                .orElseThrow(() -> new BadRequestException("Aucune session de caisse ouverte"));

        Refund refund = Refund.builder()
                .refundNumber(documentNumberService.generateRefundNumber())
                .order(order)
                .originalOrder(order)
                .refundType(request.refundType())
                .status(RefundStatus.PENDING)
                .refundMethod(request.refundMethod())
                .reason(request.reason())
                .cashier(cashier)
                .store(order.getStore())
                .shiftReport(shift)
                .notes(request.notes())
                .items(new ArrayList<>())
                .isActive(true)
                .build();

        if (request.items() != null && !request.items().isEmpty()) {
            for (RefundItemRequest itemReq : request.items()) {
                addRefundItem(refund, itemReq);
            }
        } else {
            for (OrderItem orderItem : order.getItems()) {
                RefundItem refundItem = RefundItem.builder()
                        .originalOrderItem(orderItem)
                        .product(orderItem.getProduct())
                        .quantity(orderItem.getQuantity())
                        .unitPrice(orderItem.getUnitPrice())
                        .refundAmount(orderItem.getFinalPrice())
                        .reason(request.reason())
                        .isReturned(false)
                        .build();
                refund.addItem(refundItem);
            }
        }

        refund.recalculateTotals();

        Refund saved = refundRepository.save(refund);
        log.info("Remboursement créé: {} - Montant: {}",
                saved.getRefundNumber(), saved.getTotalRefundAmount());

        return refundMapper.toResponse(saved);
    }

    private void addRefundItem(Refund refund, RefundItemRequest itemReq) {
        OrderItem orderItem = orderItemRepository.findById(itemReq.originalOrderItemId())
                .orElseThrow(() -> new NotFoundException("Article de commande non trouvé"));

        if (itemReq.quantity() > orderItem.getQuantity()) {
            throw new BadRequestException("Quantité à rembourser supérieure à la quantité achetée");
        }

        Integer alreadyRefunded = refundItemRepository.sumQuantityByProductAndCompleted(
                orderItem.getProduct().getProductId());
        if (alreadyRefunded != null) {
            int remaining = orderItem.getQuantity() - alreadyRefunded;
            if (itemReq.quantity() > remaining) {
                throw new BadRequestException("Quantité déjà remboursée pour cet article");
            }
        }

        BigDecimal unitPrice = orderItem.getUnitPrice();
        BigDecimal itemTotal = unitPrice.multiply(new BigDecimal(itemReq.quantity()));

        RefundItem refundItem = RefundItem.builder()
                .originalOrderItem(orderItem)
                .product(orderItem.getProduct())
                .quantity(itemReq.quantity())
                .unitPrice(unitPrice)
                .refundAmount(itemTotal)
                .restockingFee(itemReq.restockingFee())
                .reason(itemReq.reason())
                .isReturned(false)
                .build();

        refund.addItem(refundItem);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundById(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundByNumber(String refundNumber) {
        Refund refund = refundRepository.findByRefundNumber(refundNumber)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByOrder(UUID orderId) {
        return refundRepository.findByOrder_OrderId(orderId).stream()
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByStore(UUID storeId) {
        return refundRepository.findByStore_StoreId(storeId).stream()
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByStatus(RefundStatus status) {
        return refundRepository.findByStatus(status).stream()
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByDateRange(UUID storeId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return refundRepository.findByStoreAndDateRange(storeId, start, end).stream()
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRefundsByPeriod(UUID storeId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        BigDecimal total = refundRepository.sumCompletedRefundsByStoreAndDateRange(storeId, start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public Long countPendingRefunds() {
        return (long) refundRepository.findByStatus(RefundStatus.PENDING).size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByShift(UUID shiftReportId) {
        return refundRepository.findByShiftReport_ShiftReportId(shiftReportId).stream()
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundByOrder(UUID orderId) {
        return refundRepository.findByOrder_OrderId(orderId)
                .stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                .findFirst()
                .map(refundMapper::toResponse)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun remboursement complété trouvé pour la commande: " + orderId
                ));
    }

    @Override
    @Transactional
    public RefundResponse approveRefund(UUID refundId, UUID approverId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        refund.approve(approverId);
        Refund saved = refundRepository.save(refund);

        log.info("Remboursement {} approuvé par {}", refund.getRefundNumber(), approverId);
        return refundMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RefundResponse processRefund(UUID refundId, UUID processorId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        refund.startProcessing(processorId);
        return refundMapper.toResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional
    public RefundResponse completeRefund(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        refund.complete();
        createRefundTransaction(refund);

        // ✅ CORRECTION: Utilise la méthode correcte avec storeId
        for (RefundItem item : refund.getItems()) {
            if (item.getIsReturned()) {
                returnProductToStock(item, refund.getStore().getStoreId());
            }
        }

        if (refund.getShiftReport() != null) {
            refund.getShiftReport().addRefund(refund.getTotalRefundAmount());
        }

        Refund saved = refundRepository.save(refund);

        try {
            generateRefundPdf(saved.getRefundId());
        } catch (Exception e) {
            log.error("PDF non généré pour remboursement {} — régénérer via /api/refunds/{}/pdf",
                    saved.getRefundNumber(), saved.getRefundId(), e);
        }

        log.info("Remboursement {} complété - Montant: {}",
                saved.getRefundNumber(), saved.getTotalRefundAmount());

        return refundMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RefundResponse rejectRefund(UUID refundId, String reason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        refund.reject(reason);
        return refundMapper.toResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional
    public RefundResponse cancelRefund(UUID refundId, String reason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        refund.cancel(reason);
        return refundMapper.toResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional
    public byte[] generateRefundPdf(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        try {
            DocumentBuildContext ctx = DocumentBuildContext.forRefund(
                    refund.getOrder(), refund
            );

            byte[] pdf = builderFactory.createBuilder(DocumentType.REFUND, ctx)
                    .initialize()
                    .addHeader()
                    .addMainInfo()
                    .addItemsTable()
                    .addTotals()
                    .addFooter()
                    .build();

            String filename = refund.getRefundNumber() + ".pdf";
            fileStorageService.storeFileFromBytes(pdf, filename, refundsDirectory);

            log.info("PDF remboursement généré: {}", filename);
            return pdf;

        } catch (Exception e) {
            log.error("Erreur génération PDF remboursement {}", refundId, e);
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] regenerateRefundPdf(UUID refundId) {
        return generateRefundPdf(refundId);
    }

    // =========================================================================
    // MÉTHODES PRIVÉES
    // =========================================================================

    private void createRefundTransaction(Refund refund) {
        Transaction transaction = Transaction.builder()
                .transactionNumber(documentNumberService.generateTransactionNumber())
                .transactionType(TransactionType.REFUND)
                .amount(refund.getTotalRefundAmount())
                .paymentMethod(mapRefundMethodToPaymentMethod(refund.getRefundMethod()))
                .refund(refund)
                .cashier(refund.getCashier())
                .store(refund.getStore())
                .shiftReport(refund.getShiftReport())
                .transactionDate(LocalDateTime.now())
                .description("Remboursement " + refund.getRefundNumber())
                .isReconciled(false)
                .isVoided(false)
                .build();

        transactionRepository.save(transaction);
    }

    private PaymentMethod mapRefundMethodToPaymentMethod(RefundMethod method) {
        if (method == null) return PaymentMethod.CASH;
        return switch (method) {
            case CREDIT_CARD -> PaymentMethod.CREDIT_CARD;
            case MOBILE_MONEY -> PaymentMethod.MOBILE_MONEY;
            case BANK_TRANSFER -> PaymentMethod.BANK_TRANSFER;
            default -> PaymentMethod.CASH;
        };
    }

    // ✅ CORRECTION: Cette méthode met à jour Inventory, pas Product
    private void returnProductToStock(RefundItem item, UUID storeId) {
        if (item.getProduct() == null) return;

        inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(
                        item.getProduct().getProductId(), storeId)
                .ifPresentOrElse(
                        inv -> {
                            inv.increaseQuantity(item.getQuantity());
                            inventoryRepository.save(inv);
                            log.debug("Stock restauré: {} x{} pour {}",
                                    item.getQuantity(),
                                    item.getProduct().getName(),
                                    storeId);
                        },
                        () -> log.warn("Inventaire non trouvé pour produit {} dans store {}",
                                item.getProduct().getProductId(), storeId)
                );
    }
}