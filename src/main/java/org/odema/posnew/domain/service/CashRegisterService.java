package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.CashRegisterResponse;
import org.odema.posnew.application.dto.request.CashRegisterRequest;

import java.util.List;
import java.util.UUID;

public interface CashRegisterService {

    CashRegisterResponse createCashRegister(CashRegisterRequest request);

    CashRegisterResponse getCashRegisterById(UUID cashRegisterId);

    CashRegisterResponse getCashRegisterByNumber(String registerNumber);

    List<CashRegisterResponse> getAllCashRegistersByStore(UUID storeId);

    List<CashRegisterResponse> getActiveCashRegistersByStore(UUID storeId);

    CashRegisterResponse updateCashRegister(UUID cashRegisterId, CashRegisterRequest request);

    void deleteCashRegister(UUID cashRegisterId);

    void activateCashRegister(UUID cashRegisterId);

    void deactivateCashRegister(UUID cashRegisterId);
}
