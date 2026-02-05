package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.RefundRequest;
import org.odema.posnew.dto.response.RefundResponse;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Refund;
import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.entity.enums.RefundStatus;
import org.odema.posnew.entity.enums.RefundType;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.RefundMapper;
import org.odema.posnew.repository.*;
import org.odema.posnew.service.RefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final RefundMapper refundMapper;

    @Override
    @Transactional
    public RefundResponse createRefund(RefundRequest request, UUID cashierId) {
        // Récupérer la commande
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // Récupérer le caissier
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        // Vérifier que la commande peut être remboursée
        if (!canOrderBeRefunded(order.getOrderId())) {
            throw new BadRequestException("Cette commande ne peut pas être remboursée");
        }

        // Vérifier le montant du remboursement
        BigDecimal refundableAmount = getRefundableAmount(order.getOrderId());
        if (request.refundAmount().compareTo(refundableAmount) > 0) {
            throw new BadRequestException(
                    "Le montant du remboursement dépasse le montant remboursable. " +
                            "Montant remboursable: " + refundableAmount);
        }

        // Vérifier le type de remboursement
        RefundType refundType = request.refundType() != null ? request.refundType() : RefundType.FULL;
        if (refundType == RefundType.FULL && request.refundAmount().compareTo(refundableAmount) != 0) {
            throw new BadRequestException("Pour un remboursement complet, le montant doit être égal au montant remboursable");
        }

        // Générer un numéro de remboursement unique
        String refundNumber = generateRefundNumber();

        // Récupérer le report de shift ouvert du caissier
        Optional<ShiftReport> openShift = shiftReportRepository.findOpenShiftByCashier(cashierId);

        // Créer le remboursement
        Refund refund = Refund.builder()
                .refundNumber(refundNumber)
                .order(order)
                .refundAmount(request.refundAmount())
                .refundType(refundType)
                .reason(request.reason())
                .cashier(cashier)
                .store(order.getStore())
                .shiftReport(openShift.orElse(null))
                .notes(request.notes())
                .status(RefundStatus.PENDING)
                .isActive(true)
                .build();

        // Si un shift est ouvert, ajouter le remboursement au total
        openShift.ifPresent(shift -> {
            shift.addRefund(request.refundAmount());
            shiftReportRepository.save(shift);
        });

        Refund savedRefund = refundRepository.save(refund);
        return refundMapper.toResponse(savedRefund);
    }

    @Override
    public RefundResponse getRefundById(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        return refundMapper.toResponse(refund);
    }

    @Override
    public RefundResponse getRefundByNumber(String refundNumber) {
        Refund refund = refundRepository.findByRefundNumber(refundNumber)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse updateRefund(UUID refundId, RefundRequest request) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        // Vérifier que le remboursement peut être modifié
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Impossible de modifier un remboursement " + refund.getStatus());
        }

        // Mettre à jour les champs
        if (request.reason() != null) refund.setReason(request.reason());
        if (request.notes() != null) refund.setNotes(request.notes());

        // Mettre à jour le montant si spécifié
        if (request.refundAmount() != null) {
            // Vérifier le nouveau montant
            BigDecimal refundableAmount = getRefundableAmount(refund.getOrder().getOrderId());
            if (request.refundAmount().compareTo(refundableAmount) > 0) {
                throw new BadRequestException(
                        "Le montant du remboursement dépasse le montant remboursable. " +
                                "Montant remboursable: " + refundableAmount);
            }

            refund.setRefundAmount(request.refundAmount());
        }

        if (request.refundType() != null) refund.setRefundType(request.refundType());

        Refund updatedRefund = refundRepository.save(refund);
        return refundMapper.toResponse(updatedRefund);
    }

    @Override
    @Transactional
    public void cancelRefund(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        if (refund.getStatus() != RefundStatus.PENDING && refund.getStatus() != RefundStatus.APPROVED) {
            throw new BadRequestException("Impossible d'annuler un remboursement " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.CANCELLED);
        refund.setIsActive(false);

        // Si le remboursement était associé à un shift ouvert, ajuster le total
        if (refund.getShiftReport() != null && refund.getShiftReport().isOpen()) {
            ShiftReport shift = refund.getShiftReport();
            shift.setTotalRefunds(shift.getTotalRefunds().subtract(refund.getRefundAmount()));
            shift.calculateBalances();
            shiftReportRepository.save(shift);
        }

        refundRepository.save(refund);
    }

    @Override
    public List<RefundResponse> getAllRefunds() {
        return refundRepository.findAll().stream()
                .filter(Refund::getIsActive)
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    public List<RefundResponse> getRefundsByOrder(UUID orderId) {
        return refundRepository.findByOrder_OrderId(orderId).stream()
                .filter(Refund::getIsActive)
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    public List<RefundResponse> getRefundsByStore(UUID storeId) {
        return refundRepository.findByStore_StoreId(storeId.toString()).stream()
                .filter(Refund::getIsActive)
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    public List<RefundResponse> getRefundsByCashier(UUID cashierId) {
        return refundRepository.findByCashier_UserId(cashierId).stream()
                .filter(Refund::getIsActive)
                .map(refundMapper::toResponse)
                .toList();
    }

    @Override
    public List<RefundResponse> getRefundsByStatus(String status) {
        try {
            RefundStatus refundStatus = RefundStatus.valueOf(status.toUpperCase());
            return refundRepository.findByStatus(refundStatus).stream()
                    .filter(Refund::getIsActive)
                    .map(refundMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de remboursement invalide: " + status);
        }
    }

    @Override
    @Transactional
    public RefundResponse approveRefund(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Seuls les remboursements en attente peuvent être approuvés");
        }

        refund.approveRefund();
        Refund updatedRefund = refundRepository.save(refund);

        return refundMapper.toResponse(updatedRefund);
    }

    @Override
    @Transactional
    public RefundResponse rejectRefund(UUID refundId, String reason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BadRequestException("Seuls les remboursements en attente peuvent être rejetés");
        }

        refund.rejectRefund(reason);
        Refund updatedRefund = refundRepository.save(refund);

        return refundMapper.toResponse(updatedRefund);
    }

    @Override
    @Transactional
    public RefundResponse completeRefund(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new NotFoundException("Remboursement non trouvé"));

        if (refund.getStatus() != RefundStatus.APPROVED && refund.getStatus() != RefundStatus.PROCESSING) {
            throw new BadRequestException("Seuls les remboursements approuvés ou en cours de traitement peuvent être complétés");
        }

        refund.completeRefund();
        Refund updatedRefund = refundRepository.save(refund);

        // Mettre à jour le statut de la commande si complètement remboursée
        Order order = refund.getOrder();
        if (order.isFullyRefunded()) {
            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
        }

        return refundMapper.toResponse(updatedRefund);
    }

    @Override
    public boolean canOrderBeRefunded(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // Une commande peut être remboursée si:
        // 1. Elle est terminée
        // 2. Elle n'est pas déjà complètement remboursée
        // 3. Elle a été payée
        return order.getStatus() == OrderStatus.COMPLETED &&
                !order.isFullyRefunded() &&
                (order.getPaymentStatus() == PaymentStatus.PAID ||
                        order.getPaymentStatus() == PaymentStatus.PARTIALLY_PAID);
    }

    @Override
    public BigDecimal getRefundableAmount(UUID orderId) {
        if (!canOrderBeRefunded(orderId)) {
            return BigDecimal.ZERO;
        }

        Order order = orderRepository.findById(orderId).orElseThrow();
        BigDecimal totalRefunded = refundRepository.getTotalRefundedAmountByOrder(orderId);

        if (totalRefunded == null) {
            totalRefunded = BigDecimal.ZERO;
        }

        return order.getTotalAmount().subtract(totalRefunded);
    }

    private String generateRefundNumber() {
        String prefix = "REF";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 1000));

        String refundNumber = prefix + timestamp.substring(timestamp.length() - 6) + random;

        // Vérifier l'unicité
        while (refundRepository.findByRefundNumber(refundNumber).isPresent()) {
            random = String.valueOf((int) (Math.random() * 1000));
            refundNumber = prefix + timestamp.substring(timestamp.length() - 6) + random;
        }

        return refundNumber;
    }
}
