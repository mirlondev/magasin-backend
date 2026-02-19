package org.odema.posnew.mapper;

import org.odema.posnew.dto.response.CashRegisterResponse;
import org.odema.posnew.entity.CashRegister;
import org.springframework.stereotype.Component;

@Component
public class CashRegisterMapper {

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
}
