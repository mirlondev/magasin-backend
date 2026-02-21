package org.odema.posnew.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.application.dto.response.PaymentResponse;
import org.odema.posnew.domain.enums_old.OrderStatus;
import org.odema.posnew.domain.enums_old.PaymentMethod;
import org.odema.posnew.domain.enums_old.PaymentStatus;
import org.odema.posnew.domain.enums_old.UserRole;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.mapper.PaymentMapper;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.repository.PaymentRepository;
import org.odema.posnew.repository.ShiftReportRepository;
import org.odema.posnew.repository.UserRepository;
import org.odema.posnew.application.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID orderId, PaymentRequest request, UUID cashierId) throws UnauthorizedException {
        // Récupérer la commande
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // Vérifications de base
        validateOrderForPayment(order);
        validatePaymentAmount(request.amount(), order);

        // Récupérer le caissier
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        // Vérifier les permissions pour crédit
        if (request.method() == PaymentMethod.CREDIT) {
            validateCreditPermission(cashier);
        }

        // Récupérer le shift report ouvert
        Optional<ShiftReport> shiftOpt = shiftReportRepository.findOpenShiftByCashier(cashierId);
        ShiftReport shift = shiftOpt.orElse(null);

        // Si pas de shift et paiement réel (non crédit), erreur
        if (shift == null && request.method() != PaymentMethod.CREDIT) {
            throw new BadRequestException("Aucun shift ouvert pour ce caissier. Veuillez ouvrir un shift.");
        }

        // Créer le paiement
        Payment payment = Payment.builder()
                .order(order)
                .method(request.method())
                .amount(request.amount())
                .cashier(cashier)
                .shiftReport(shift)
                .status(request.method() == PaymentMethod.CREDIT ?
                        PaymentStatus.CREDIT : PaymentStatus.PAID)
                .notes(request.notes())
                .isActive(true)
                .build();

        // Sauvegarder le paiement
        Payment savedPayment = paymentRepository.save(payment);

        // Ajouter le paiement à la commande
        order.addPayment(savedPayment);

        // Mettre à jour le shift report (sauf pour crédit)
        if (shift != null && request.method() != PaymentMethod.CREDIT) {
            shift.addSale(request.amount(),request.method());
            shiftReportRepository.save(shift);
        }

        // Si la commande est maintenant complètement payée, la marquer comme terminée
        if (order.getPaymentStatus() == PaymentStatus.PAID &&
                order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(java.time.LocalDateTime.now());
        }

        // Sauvegarder la commande mise à jour
        orderRepository.save(order);

        log.info("Paiement créé: {} FCFA via {} pour commande {}",
                request.amount(), request.method(), order.getOrderNumber());

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse createCreditPayment(UUID orderId, PaymentRequest request, UUID managerId) throws UnauthorizedException {
        // Forcer la méthode CREDIT
        if (request.method() != PaymentMethod.CREDIT) {
            throw new BadRequestException("Cette méthode est uniquement pour les crédits");
        }

        // Vérifier les permissions du manager
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new NotFoundException("Manager non trouvé"));

        validateCreditPermission(manager);

        // Créer le paiement crédit
        return processPayment(orderId, request, managerId);
    }

    @Override
    public List<PaymentResponse> getOrderPayments(UUID orderId) {
        // Vérifier que la commande existe
        orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        List<Payment> payments = paymentRepository.findByOrder_OrderId(orderId);
        return payments.stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé"));

        // Vérifier si le paiement peut être annulé
        if (!payment.getIsActive()) {
            throw new BadRequestException("Ce paiement est déjà annulé");
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BadRequestException("Ce paiement est déjà annulé");
        }

        // Annuler le paiement
        payment.cancel();
        paymentRepository.save(payment);

        // Mettre à jour le shift report si nécessaire
        if (payment.getShiftReport() != null && payment.getMethod() != PaymentMethod.CREDIT) {
            ShiftReport shift = payment.getShiftReport();
            // Soustraire le montant des ventes et l'ajouter aux remboursements
            shift.setTotalSales(shift.getTotalSales().subtract(payment.getAmount()));
            shift.setTotalRefunds(shift.getTotalRefunds().add(payment.getAmount()));
            shift.calculateBalances();
            shiftReportRepository.save(shift);
        }

        // Mettre à jour le statut de la commande
        Order order = payment.getOrder();
        order.removePayment(payment);
        order.setPaymentStatus(order.getComputedPaymentStatus());
        orderRepository.save(order);

        log.info("Paiement {} annulé pour commande {}", paymentId, order.getOrderNumber());
    }

    @Override
    public BigDecimal getOrderTotalPaid(UUID orderId) {
        return paymentRepository.getTotalPaidByOrder(orderId);
    }

    @Override
    public BigDecimal getOrderCreditAmount(UUID orderId) {
        return paymentRepository.getTotalCreditByOrder(orderId);
    }

    @Override
    public BigDecimal getOrderRemainingAmount(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        return order.getRemainingAmount();
    }

    // ============ MÉTHODES PRIVÉES DE VALIDATION ============

    /**
     * Valide qu'une commande peut recevoir un paiement
     */
    private void validateOrderForPayment(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible d'ajouter un paiement à une commande annulée");
        }

        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw new BadRequestException("Impossible d'ajouter un paiement à une commande remboursée");
        }
    }

    /**
     * Valide le montant du paiement
     */
    private void validatePaymentAmount(BigDecimal amount, Order order) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant du paiement doit être supérieur à zéro");
        }

        BigDecimal remaining = order.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new BadRequestException(
                    String.format("Montant trop élevé. Montant restant: %s FCFA", remaining)
            );
        }
    }

    /**
     * Valide les permissions pour créer un crédit
     */
    private void validateCreditPermission(User user) throws UnauthorizedException {
        if (user.getUserRole() == UserRole.CASHIER) {
            throw new UnauthorizedException(
                    "Les caissiers ne peuvent pas créer des crédits. " +
                            "Contactez un manager ou un administrateur."
            );
        }

        if (user.getUserRole() != UserRole.ADMIN &&
                user.getUserRole() != UserRole.STORE_ADMIN )
            throw new UnauthorizedException("Permission insuffisante pour créer un crédit");
        }
    }
