package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.ShiftReportRequest;
import org.odema.posnew.dto.response.ShiftReportDetailResponse;
import org.odema.posnew.dto.response.ShiftReportResponse;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.entity.Store;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.entity.enums.ShiftStatus;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.ShiftReportMapper;
import org.odema.posnew.repository.PaymentRepository;
import org.odema.posnew.repository.ShiftReportRepository;
import org.odema.posnew.repository.StoreRepository;
import org.odema.posnew.repository.UserRepository;
import org.odema.posnew.service.ShiftReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShiftReportServiceImpl implements ShiftReportService {

    private final ShiftReportRepository shiftReportRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ShiftReportMapper shiftReportMapper;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public ShiftReportResponse openShift(ShiftReportRequest request, UUID cashierId) {

        if (shiftReportRepository.findOpenShiftByCashier(cashierId).isPresent()) {
            throw new BadRequestException("Le caissier a déjà un shift ouvert");
        }

        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        BigDecimal openingBalance =
                request.openingBalance() != null ? request.openingBalance() : BigDecimal.ZERO;

        String shiftNumber = generateShiftNumber();

        ShiftReport shift = ShiftReport.builder()
                .shiftNumber(shiftNumber)
                .cashier(cashier)
                .store(store)
                .startTime(LocalDateTime.now())

                .openingBalance(openingBalance)
                .actualBalance(openingBalance)
                .expectedBalance(openingBalance)
                .closingBalance(openingBalance)
                .discrepancy(BigDecimal.ZERO)

                .totalSales(BigDecimal.ZERO)
                .totalRefunds(BigDecimal.ZERO)
                .netSales(BigDecimal.ZERO)
                .totalTransactions(0)

                .status(ShiftStatus.OPEN)
                .notes(request.notes())
                .build();

        ShiftReport savedShift = shiftReportRepository.save(shift);
        return shiftReportMapper.toResponse(savedShift);
    }
  /*  @Override
    @Transactional
    public ShiftReportResponse closeShift(UUID shiftReportId, BigDecimal closingBalance, BigDecimal actualBalance) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        if (shift.isClosed()) {
            throw new BadRequestException("Le shift est déjà fermé");
        }

        shift.setClosingBalance(closingBalance);
        shift.setActualBalance(actualBalance);
        shift.closeShift();

        ShiftReport updatedShift = shiftReportRepository.save(shift);
        return shiftReportMapper.toResponse(updatedShift);
    }
*/
    @Override
    public ShiftReportResponse getShiftReportById(UUID shiftReportId) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        return shiftReportMapper.toResponse(shift);
    }

    @Override
    public ShiftReportResponse getShiftReportByNumber(String shiftNumber) {
        ShiftReport shift = shiftReportRepository.findByShiftNumber(shiftNumber)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        return shiftReportMapper.toResponse(shift);
    }

    @Override
    public List<ShiftReportResponse> getShiftsByCashier(UUID cashierId) {
        return shiftReportRepository.findByCashier_UserId(cashierId).stream()
                .map(shiftReportMapper::toResponse)
                .toList();
    }

    @Override
    public List<ShiftReportResponse> getShiftsByStore(UUID storeId) {
        return shiftReportRepository.findByStore_StoreId(storeId).stream()
                .map(shiftReportMapper::toResponse)
                .toList();
    }

    @Override
    public List<ShiftReportResponse> getShiftsByStatus(String status) {
        try {
            ShiftStatus shiftStatus = ShiftStatus.valueOf(status.toUpperCase());
            return shiftReportRepository.findByStatus(shiftStatus).stream()
                    .map(shiftReportMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de shift invalide: " + status);
        }
    }

    @Override
    public List<ShiftReportResponse> getShiftsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return shiftReportRepository.findByDateRange(startDate, endDate).stream()
                .map(shiftReportMapper::toResponse)
                .toList();
    }

    @Override
    public ShiftReportResponse getOpenShiftByCashier(UUID cashierId) {
        ShiftReport shift = shiftReportRepository.findOpenShiftByCashier(cashierId)
                .orElseThrow(() -> new NotFoundException("Aucun shift ouvert trouvé pour ce caissier"));

        return shiftReportMapper.toResponse(shift);
    }

    @Override
    public List<ShiftReportResponse> getOpenShiftsByStore(UUID storeId) {
        return shiftReportRepository.findOpenShiftsByStore(storeId).stream()
                .map(shiftReportMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ShiftReportResponse updateShiftReport(UUID shiftReportId, ShiftReportRequest request) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        if (shift.isClosed()) {
            throw new BadRequestException("Impossible de modifier un shift fermé");
        }

        // Mettre à jour les champs
        if (request.notes() != null) shift.setNotes(request.notes());
        if (request.openingBalance() != null) shift.setOpeningBalance(request.openingBalance());

        // Recalculer les soldes
        shift.calculateBalances();

        ShiftReport updatedShift = shiftReportRepository.save(shift);
        return shiftReportMapper.toResponse(updatedShift);
    }

    @Override
    @Transactional
    public void suspendShift(UUID shiftReportId, String reason) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        if (shift.isClosed()) {
            throw new BadRequestException("Impossible de suspendre un shift fermé");
        }

        shift.setStatus(ShiftStatus.SUSPENDED);
        if (reason != null) {
            shift.setNotes((shift.getNotes() != null ? shift.getNotes() + "\n" : "") +
                    "Suspendu: " + reason);
        }

        shiftReportRepository.save(shift);
    }

    @Override
    @Transactional
    public ShiftReportResponse resumeShift(UUID shiftReportId) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        if (shift.isClosed()) {
            throw new BadRequestException("Impossible de reprendre un shift fermé");
        }

        if (shift.getStatus() != ShiftStatus.SUSPENDED) {
            throw new BadRequestException("Le shift n'est pas suspendu");
        }

        shift.setStatus(ShiftStatus.OPEN);
        ShiftReport updatedShift = shiftReportRepository.save(shift);

        return shiftReportMapper.toResponse(updatedShift);
    }

    @Override
    public BigDecimal getTotalSalesByStore(UUID storeId) {
        Double total = shiftReportRepository.getTotalSalesByStore(storeId);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalRefundsByStore(UUID storeId) {
        Double total = shiftReportRepository.getTotalRefundsByStore(storeId);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    private String generateShiftNumber() {
        String prefix = "SHIFT";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 1000));

        String shiftNumber = prefix + timestamp.substring(timestamp.length() - 6) + random;

        // Vérifier l'unicité
        while (shiftReportRepository.findByShiftNumber(shiftNumber).isPresent()) {
            random = String.valueOf((int) (Math.random() * 1000));
            shiftNumber = prefix + timestamp.substring(timestamp.length() - 6) + random;
        }

        return shiftNumber;
    }






    // Modifier la méthode closeShift pour inclure les totaux par méthode
    @Transactional
    @Override
    public ShiftReportResponse closeShift(UUID shiftReportId, BigDecimal closingBalance, BigDecimal actualBalance) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        if (shift.isClosed()) {
            throw new BadRequestException("Le shift est déjà fermé");
        }

        // Calculer les totaux par méthode de paiement
        BigDecimal cashTotal = getCashTotal(shiftReportId);
        BigDecimal mobileTotal = getMobileTotal(shiftReportId);
        BigDecimal cardTotal = getCardTotal(shiftReportId);

        shift.setClosingBalance(closingBalance);
        shift.setActualBalance(actualBalance);

        // Ajouter les totaux détaillés (vous pouvez les stocker dans un champ JSON ou créer une nouvelle entité)
        String details = String.format("Cash: %s, Mobile: %s, Card: %s",
                cashTotal, mobileTotal, cardTotal);
        shift.setNotes(shift.getNotes() != null ?
                shift.getNotes() + "\n" + details : details);

        shift.closeShift();

        ShiftReport updatedShift = shiftReportRepository.save(shift);
        return shiftReportMapper.toResponse(updatedShift);
}

//new methods

    @Transactional
    @Override
    public ShiftReportDetailResponse getShiftDetail(UUID shiftReportId) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        // Récupérer tous les paiements de ce shift
        List<Payment> payments = paymentRepository.findByShiftReport_ShiftReportId(shiftReportId);

        // Calculer les totaux par méthode de paiement
        Map<PaymentMethod, BigDecimal> paymentsByMethod = new HashMap<>();
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalMobile = BigDecimal.ZERO;
        BigDecimal totalCard = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;

        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                switch (payment.getMethod()) {
                    case CASH -> totalCash = totalCash.add(payment.getAmount());
                    case MOBILE_MONEY -> totalMobile = totalMobile.add(payment.getAmount());
                    case CREDIT_CARD -> totalCard = totalCard.add(payment.getAmount());
                    default -> {
                        // Pour les autres méthodes
                        paymentsByMethod.merge(payment.getMethod(), payment.getAmount(), BigDecimal::add);
                    }
                }
                totalSales = totalSales.add(payment.getAmount());
            } else if (payment.getStatus() == PaymentStatus.CREDIT) {
                totalCredit = totalCredit.add(payment.getAmount());
            }
        }

        // Compter les transactions
        long transactionCount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .count();

        return ShiftReportDetailResponse.builder()
                .shiftReportId(shift.getShiftReportId())
                .shiftNumber(shift.getShiftNumber())
                .cashierId(shift.getCashier().getUserId())
                .cashierName(shift.getCashier().getUsername())
                .storeId(shift.getStore().getStoreId())
                .storeName(shift.getStore().getName())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .openingBalance(shift.getOpeningBalance())
                .closingBalance(shift.getClosingBalance())
                .expectedBalance(shift.getExpectedBalance())
                .actualBalance(shift.getActualBalance())
                .discrepancy(shift.getDiscrepancy())
                .totalTransactions((int) transactionCount)
                .totalSales(totalSales)
                .totalRefunds(shift.getTotalRefunds())
                .netSales(shift.getNetSales())
                .cashTotal(totalCash)
                .mobileTotal(totalMobile)
                .cardTotal(totalCard)
                .creditTotal(totalCredit)
                .otherPayments(paymentsByMethod)
                .notes(shift.getNotes())
                .status(shift.getStatus())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }

    @Override
    public BigDecimal getCashTotal(UUID shiftId) {
        return paymentRepository.sumByMethodAndShift(PaymentMethod.CASH, shiftId);
    }

    @Override
    public BigDecimal getMobileTotal(UUID shiftId) {
        return paymentRepository.sumByMethodAndShift(PaymentMethod.MOBILE_MONEY, shiftId);
    }

    @Override
    public BigDecimal getCardTotal(UUID shiftId) {
        return paymentRepository.sumByMethodAndShift(PaymentMethod.CREDIT_CARD, shiftId);
    }

    @Override
    public BigDecimal getCreditTotal(UUID shiftId) {
        return paymentRepository.sumByMethodAndShift(PaymentMethod.CREDIT, shiftId);
    }

    // Mettre à jour la méthode closeShift pour utiliser les paiements
    @Transactional
    @Override
    public ShiftReportResponse closeShift(UUID shiftReportId, BigDecimal actualBalance, String notes) {
        ShiftReport shift = shiftReportRepository.findById(shiftReportId)
                .orElseThrow(() -> new NotFoundException("Shift non trouvé"));

        if (shift.isClosed()) {
            throw new BadRequestException("Le shift est déjà fermé");
        }

        // Calculer les totaux à partir des paiements
        BigDecimal totalSales = calculateTotalSalesFromPayments(shiftReportId);
        BigDecimal totalRefunds = shift.getTotalRefunds();

        // Calculer le solde attendu
        BigDecimal expectedBalance = shift.getOpeningBalance()
                .add(totalSales)
                .subtract(totalRefunds);

        // Définir le solde de clôture (si actualBalance est null, utiliser expectedBalance)
        BigDecimal closingBalance = actualBalance != null ? actualBalance : expectedBalance;

        // Calculer l'écart
        BigDecimal discrepancy = closingBalance.subtract(expectedBalance);

        // Mettre à jour le shift
        shift.setClosingBalance(closingBalance);
        shift.setActualBalance(closingBalance);
        shift.setExpectedBalance(expectedBalance);
        shift.setDiscrepancy(discrepancy);
        shift.setTotalSales(totalSales);
        shift.setNetSales(totalSales.subtract(totalRefunds));
        shift.setTotalTransactions(countTransactions(shiftReportId));

        if (notes != null) {
            String existingNotes = shift.getNotes() != null ? shift.getNotes() + "\n" : "";
            shift.setNotes(existingNotes + "Fermeture: " + notes);
        }

        shift.closeShift();

        ShiftReport updatedShift = shiftReportRepository.save(shift);
        return shiftReportMapper.toResponse(updatedShift);
    }

    private BigDecimal calculateTotalSalesFromPayments(UUID shiftId) {
        BigDecimal total = BigDecimal.ZERO;

        // Somme des paiements CASH, MOBILE_MONEY, CARD (statut PAID)
        total = total.add(paymentRepository.sumByMethodAndShift(PaymentMethod.CASH, shiftId));
        total = total.add(paymentRepository.sumByMethodAndShift(PaymentMethod.MOBILE_MONEY, shiftId));
        total = total.add(paymentRepository.sumByMethodAndShift(PaymentMethod.CREDIT_CARD, shiftId));
        total = total.add(paymentRepository.sumByMethodAndShift(PaymentMethod.BANK_TRANSFER, shiftId));
        total = total.add(paymentRepository.sumByMethodAndShift(PaymentMethod.CHECK, shiftId));
        total = total.add(paymentRepository.sumByMethodAndShift(PaymentMethod.LOYALTY_POINTS, shiftId));

        return total;
    }

    private Integer countTransactions(UUID shiftId) {
        return Math.toIntExact(paymentRepository.countByShiftReport_ShiftReportIdAndStatus(
                shiftId,
                PaymentMethod.CASH));
    }

}