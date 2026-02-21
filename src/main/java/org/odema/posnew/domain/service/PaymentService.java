package org.odema.posnew.application.service;

import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.application.dto.response.PaymentResponse;
import org.odema.posnew.api.exception.UnauthorizedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des paiements
 */
public interface PaymentService {

    /**
     * Traiter un paiement pour une commande
     *
     * @param orderId ID de la commande
     * @param request Détails du paiement
     * @param cashierId ID du caissier effectuant le paiement
     * @return Le paiement créé
     */
    PaymentResponse processPayment(UUID orderId, PaymentRequest request, UUID cashierId) throws UnauthorizedException;

    /**
     * Créer un paiement crédit (réservé aux managers)
     *
     * @param orderId ID de la commande
     * @param request Détails du crédit
     * @param managerId ID du manager
     * @return Le paiement crédit créé
     */
    PaymentResponse createCreditPayment(UUID orderId, PaymentRequest request, UUID managerId) throws UnauthorizedException;

    /**
     * Obtenir tous les paiements d'une commande
     *
     * @param orderId ID de la commande
     * @return Liste des paiements
     */
    List<PaymentResponse> getOrderPayments(UUID orderId);

    /**
     * Annuler un paiement
     *
     * @param paymentId ID du paiement
     */
    void cancelPayment(UUID paymentId);

    /**
     * Obtenir le total payé pour une commande (exclut crédit)
     *
     * @param orderId ID de la commande
     * @return Montant total payé
     */
    BigDecimal getOrderTotalPaid(UUID orderId);

    /**
     * Obtenir le montant en crédit pour une commande
     *
     * @param orderId ID de la commande
     * @return Montant en crédit
     */
    BigDecimal getOrderCreditAmount(UUID orderId);

    /**
     * Obtenir le montant restant à payer
     *
     * @param orderId ID de la commande
     * @return Montant restant
     */
    BigDecimal getOrderRemainingAmount(UUID orderId);
}