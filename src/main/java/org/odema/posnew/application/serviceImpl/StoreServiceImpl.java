package org.odema.posnew.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.domain.enums_old.StoreStatus;
import org.odema.posnew.domain.enums_old.StoreType;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.mapper.StoreMapper;
import org.odema.posnew.repository.StoreRepository;
import org.odema.posnew.application.service.StoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    @Override
    @Transactional
    public StoreDto createStore(StoreDto storeDto, User user) throws UnauthorizedException {
        // Vérifier les permissions
        if (!user.getUserRole().name().equals("ADMIN") &&
                !user.getUserRole().name().equals("STORE_MANAGER")) {
            throw new UnauthorizedException("Permission insuffisante pour créer un store");
        }

        Store store = storeMapper.toEntity(storeDto);
        store.setCreatedAt(LocalDateTime.now());
        store.setStatus(StoreStatus.PENDING);
        store.setStoreType(storeDto.storeType() != null ? storeDto.storeType() : StoreType.SHOP);
        store.setIsActive(true);

        Store savedStore = storeRepository.save(store);
        return storeMapper.toDto(savedStore);
    }

    @Override
    public StoreDto findStoreById(UUID storeId) throws NotFoundException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé avec ID: " + storeId));

        return storeMapper.toDto(store);
    }

    @Override
    @Transactional
    public StoreDto updateStore(UUID storeId, StoreDto storeDto) throws NotFoundException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        // Mettre à jour les champs
        if (storeDto.name() != null) store.setName(storeDto.name());
        if (storeDto.city() != null) store.setCity(storeDto.city());
        if (storeDto.postalCode() != null) store.setPostalCode(storeDto.postalCode());
        if (storeDto.country() != null) store.setCountry(storeDto.country());
        if (storeDto.storeType() != null) store.setStoreType(storeDto.storeType());
        if (storeDto.status() != null) store.setStatus(storeDto.status());
        if (storeDto.phone() != null) store.setPhone(storeDto.phone());
        if (storeDto.email() != null) store.setEmail(storeDto.email());
        if (storeDto.openingHours() != null) store.setOpeningHours(storeDto.openingHours());
        if (storeDto.latitude() != null) store.setLatitude(storeDto.latitude());
        if (storeDto.longitude() != null) store.setLongitude(storeDto.longitude());
        if (storeDto.isActive() != null) store.setIsActive(storeDto.isActive());

        store.setUpdatedAt(LocalDateTime.now());

        Store updatedStore = storeRepository.save(store);
        return storeMapper.toDto(updatedStore);
    }

    @Override
    @Transactional
    public void deleteStore(UUID storeId) throws NotFoundException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        // Soft delete
        store.setIsActive(false);
        store.setStatus(StoreStatus.CLOSED);
        store.setUpdatedAt(LocalDateTime.now());

        storeRepository.save(store);
    }

    @Override
    public List<StoreDto> getAllStores() {
        return storeRepository.findAllByIsActiveTrue()
                .stream()
                .map(storeMapper::toDto)
                .toList();
    }

    @Override
    public List<StoreDto> getStoresByType(StoreType storeType) {
        return storeRepository.findByStoreTypeAndIsActiveTrue(storeType)
                .stream()
                .map(storeMapper::toDto)
                .toList();
    }
}