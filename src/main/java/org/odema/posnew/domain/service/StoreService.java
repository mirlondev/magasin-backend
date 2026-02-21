package org.odema.posnew.domain.service;

import jakarta.validation.Valid;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.dto.request.StoreRequest;
import org.odema.posnew.application.dto.response.StoreResponse;
import org.odema.posnew.domain.model.User;
import org.odema.posnew.domain.model.enums.StoreType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface StoreService {
    StoreResponse createStore(StoreRequest storeDto, User user) throws UnauthorizedException;
    StoreResponse findStoreById(UUID storeId) throws NotFoundException;

    @Transactional
    StoreResponse updateStore(UUID storeId, @Valid StoreRequest storeDto) throws NotFoundException;

    void deleteStore(UUID storeId) throws NotFoundException;
    List<StoreResponse> getAllStores();

    List<StoreResponse> getStoresByType(StoreType storeType);
}
