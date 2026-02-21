package org.odema.posnew.application.serviceImpl;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.api.exception.BusinessException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.CashRegisterResponse;
import org.odema.posnew.application.dto.request.CashRegisterRequest;
import org.odema.posnew.application.mapper.CashRegisterMapper;
import org.odema.posnew.domain.model.CashRegister;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.repository.CashRegisterRepository;
import org.odema.posnew.domain.repository.StoreRepository;
import org.odema.posnew.domain.service.CashRegisterService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashRegisterServiceImpl implements CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final StoreRepository storeRepository;
    private  final CashRegisterMapper cashRegisterMapper;

    @Override
    @Transactional
    public CashRegisterResponse createCashRegister(CashRegisterRequest request) {
        if (cashRegisterRepository.existsByRegisterNumber(request.registerNumber())) {
            throw new BusinessException("Ce numéro de caisse existe déjà");
        }

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        CashRegister register = CashRegister.builder()
                .registerNumber(request.registerNumber())
                .name(request.name())
                .store(store)
                .location(request.location())
                .isActive(true)
                .build();

        CashRegister saved = cashRegisterRepository.save(register);
        return cashRegisterMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CashRegisterResponse getCashRegisterById(UUID cashRegisterId) {
        CashRegister register = cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> new NotFoundException("Caisse non trouvée"));
        return cashRegisterMapper.toResponse(register);
    }

    @Override
    @Transactional(readOnly = true)
    public CashRegisterResponse getCashRegisterByNumber(String registerNumber) {
        CashRegister register = cashRegisterRepository.findByRegisterNumber(registerNumber)
                .orElseThrow(() -> new NotFoundException("Caisse non trouvée"));
        return cashRegisterMapper.toResponse(register);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashRegisterResponse> getAllCashRegistersByStore(UUID storeId) {
        return cashRegisterRepository.findByStore_StoreId(storeId).stream()
                .map(cashRegisterMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashRegisterResponse> getActiveCashRegistersByStore(UUID storeId) {
        return cashRegisterRepository.findByStore_StoreIdAndIsActiveTrue(storeId).stream()
                .map(cashRegisterMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CashRegisterResponse updateCashRegister(UUID cashRegisterId, CashRegisterRequest request) {
        CashRegister register = cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> new NotFoundException("Caisse non trouvée"));

        // Vérifier l'unicité du numéro si modifié
        if (!register.getRegisterNumber().equals(request.registerNumber()) &&
                cashRegisterRepository.existsByRegisterNumber(request.registerNumber())) {
            throw new BusinessException("Ce numéro de caisse existe déjà");
        }

        register.setRegisterNumber(request.registerNumber());
        register.setName(request.name());
        register.setLocation(request.location());

        if (request.storeId() != null && !register.getStore().getStoreId().equals(request.storeId())) {
            Store store = storeRepository.findById(request.storeId())
                    .orElseThrow(() -> new NotFoundException("Store non trouvé"));
            register.setStore(store);
        }

        CashRegister updated = cashRegisterRepository.save(register);
        return cashRegisterMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCashRegister(UUID cashRegisterId) {
        CashRegister register = cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> new NotFoundException("Caisse non trouvée"));

        // Soft delete
        register.setIsActive(false);
        cashRegisterRepository.save(register);
    }

    @Override
    @Transactional
    public void activateCashRegister(UUID cashRegisterId) {
        CashRegister register = cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> new NotFoundException("Caisse non trouvée"));
        register.setIsActive(true);
        cashRegisterRepository.save(register);
    }

    @Override
    @Transactional
    public void deactivateCashRegister(UUID cashRegisterId) {
        CashRegister register = cashRegisterRepository.findById(cashRegisterId)
                .orElseThrow(() -> new NotFoundException("Caisse non trouvée"));
        register.setIsActive(false);
        cashRegisterRepository.save(register);
    }
}