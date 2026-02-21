package org.odema.posnew.application.service;

import org.odema.posnew.domain.enums_old.StoreType;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;

import java.util.List;
import java.util.UUID;

public interface StoreService {
    StoreDto createStore(StoreDto storeDto, User user) throws UnauthorizedException;
    StoreDto findStoreById(UUID storeId) throws NotFoundException;
    StoreDto updateStore(UUID storeId, StoreDto storeDto) throws NotFoundException;
    void deleteStore(UUID storeId) throws NotFoundException;
    List<StoreDto> getAllStores();

    List<StoreDto> getStoresByType(StoreType storeType);
}
