package org.odema.posnew.service;

import org.odema.posnew.dto.StoreDto;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.StoreType;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.exception.UnauthorizedException;

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
