package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.request.RefundRequest;
import org.odema.posnew.application.dto.response.RefundResponse;

import org.odema.posnew.domain.model.enums.RefundStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service pour la gestion des remboursements.
 */
public interface RefundService {

    /**
     * Crée un nouveau remboursement
     */
    RefundResponse createRefund(RefundRequest request, UUID cashierId);

    /**
     * Récupère un remboursement par ID
     */
    RefundResponse getRefundById(UUID refundId);

    /**
     * Récupère un remboursement par numéro
     */
    RefundResponse getRefundByNumber(String refundNumber);

    /**
     * Liste les remboursements d'une commande
     */
    List<RefundResponse> getRefundsByOrder(UUID orderId);

    /**
     * Liste les remboursements d'un magasin
     */
    List<RefundResponse> getRefundsByStore(UUID storeId);

    /**
     * Liste les remboursements par statut
     */
    List<RefundResponse> getRefundsByStatus(RefundStatus status);

//    @Transactional(readOnly = true)
//    List<RefundResponse> getRefundsByStatus(RefundStatus status);

    /**
     * Liste les remboursements d'une période
     */
    List<RefundResponse> getRefundsByDateRange(UUID storeId, LocalDate startDate, LocalDate endDate);

    /**
     * Approuve un remboursement
     */
    RefundResponse approveRefund(UUID refundId, UUID approverId);

    /**
     * Traite un remboursement approuvé
     */
    RefundResponse processRefund(UUID refundId, UUID processorId);

    /**
     * Complète un remboursement en cours
     */
    RefundResponse completeRefund(UUID refundId);

    /**
     * Rejette un remboursement
     */
    RefundResponse rejectRefund(UUID refundId, String reason);

    /**
     * Annule un remboursement
     */
    RefundResponse cancelRefund(UUID refundId, String reason);

    /**
     * Génère le PDF du remboursement
     */
    byte[] generateRefundPdf(UUID refundId);

    /**
     * Régénère le PDF d'un remboursement
     */
    byte[] regenerateRefundPdf(UUID refundId);

    /**
     * Retourne le montant total des remboursements d'une période
     */
    BigDecimal getTotalRefundsByPeriod(UUID storeId, LocalDate startDate, LocalDate endDate);

    /**
     * Compte les remboursements en attente
     */
    Long countPendingRefunds();

    /**
     * Liste les remboursements d'une session de caisse
     */
    List<RefundResponse> getRefundsByShift(UUID shiftReportId);



    /**
     * Retourne le premier remboursement COMPLETED pour une commande.
     * Utilisé par DocumentGenerationFacade pour générer le PDF de remboursement
     * depuis une Order sans connaître le refundId.
     */
    RefundResponse getRefundByOrder(UUID orderId);


}
