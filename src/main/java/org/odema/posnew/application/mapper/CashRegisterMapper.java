package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.CashRegisterResponse;
import org.odema.posnew.application.dto.request.CashRegisterRequest;
import org.odema.posnew.domain.model.CashRegister;
import org.odema.posnew.domain.model.Store;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CashRegisterMapper {

    public CashRegister toEntity(CashRegisterRequest request, Store store) {
        if (request == null) return null;

        return CashRegister.builder()
                .registerNumber(request.registerNumber())
                .name(request.name())
                .store(store)
                .location(request.location())
                .model(request.model())
                .serialNumber(request.serialNumber())
                .isActive(true)
                .build();
    }

    public CashRegisterResponse toResponse(CashRegister register) {
        if (register == null) return null;

        return new CashRegisterResponse(
                register.getCashRegisterId(),
                register.getRegisterNumber(),
                register.getName(),
                register.getStore() != null ? register.getStore().getStoreId() : null,
                register.getStore() != null ? register.getStore().getName() : null,
                register.getIsActive(),
                register.getLocation(),
                register.getCreatedAt(),
                register.getUpdatedAt()
        );
    }

    public List<CashRegisterResponse> toResponseList(List<CashRegister> registers) {
        if (registers == null) return List.of();
        return registers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntityFromRequest(CashRegister register, CashRegisterRequest request) {
        if (request == null || register == null) return;

        if (request.registerNumber() != null) register.setRegisterNumber(request.registerNumber());
        if (request.name() != null) register.setName(request.name());
        if (request.location() != null) register.setLocation(request.location());
        if (request.model() != null) register.setModel(request.model());
        if (request.serialNumber() != null) register.setSerialNumber(request.serialNumber());
        if (request.storeId() != null) {
            // Store is handled separately in service layer
        }
    }
}