package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.ShiftReportRequest;
import org.odema.posnew.dto.response.ShiftReportResponse;
import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.entity.Store;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.ShiftStatus;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.ShiftReportMapper;
import org.odema.posnew.repository.ShiftReportRepository;
import org.odema.posnew.repository.StoreRepository;
import org.odema.posnew.repository.UserRepository;
import org.odema.posnew.service.ShiftReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShiftReportServiceImpl implements ShiftReportService {

    private final ShiftReportRepository shiftReportRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ShiftReportMapper shiftReportMapper;

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
    @Override
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
        return shiftReportRepository.findOpenShiftsByStore(storeId.toString()).stream()
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
        Double total = shiftReportRepository.getTotalSalesByStore(storeId.toString());
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalRefundsByStore(UUID storeId) {
        Double total = shiftReportRepository.getTotalRefundsByStore(storeId.toString());
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
}