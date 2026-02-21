package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.dto.request.StoreRequest;
import org.odema.posnew.application.dto.response.StoreResponse;
import org.odema.posnew.application.mapper.StoreMapper;

import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.User;
import org.odema.posnew.domain.model.enums.StoreStatus;
import org.odema.posnew.domain.model.enums.StoreType;
import org.odema.posnew.domain.repository.StoreRepository;
import org.odema.posnew.domain.service.StoreService;
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
    public StoreResponse createStore(StoreRequest storeDto , User user) throws UnauthorizedException {
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
        return storeMapper.toResponse(savedStore);
    }

    @Override
    public StoreResponse findStoreById(UUID storeId) throws NotFoundException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé avec ID: " + storeId));

        return storeMapper.toResponse(store);
    }




    @Override
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAllByIsActiveTrue()
                .stream()
                .map(storeMapper::toResponse)
                .toList();
    }

    @Override
    public List<StoreResponse> getStoresByType(StoreType storeType) {
        return storeRepository.findByStoreTypeAndIsActiveTrue(storeType)
                .stream()
                .map(storeMapper::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public StoreResponse updateStore(UUID storeId,  StoreRequest storeDto) throws NotFoundException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        if (storeDto.name() != null)         store.setName(storeDto.name());
        getDtoData(storeDto, store);

        // ✅ SUPPRIMÉ : store.setUpdatedAt(LocalDateTime.now())
        // @UpdateTimestamp le fait automatiquement

        return storeMapper.toResponse(storeRepository.save(store));
    }

    public static void getDtoData(StoreRequest storeDto, Store store) {
        if (storeDto.city() != null)         store.setCity(storeDto.city());
        if (storeDto.postalCode() != null)   store.setPostalCode(storeDto.postalCode());
        if (storeDto.country() != null)      store.setCountry(storeDto.country());
        if (storeDto.storeType() != null)    store.setStoreType(storeDto.storeType());
        if (storeDto.status() != null)       store.setStatus(storeDto.status());
        if (storeDto.phone() != null)        store.setPhone(storeDto.phone());
        if (storeDto.email() != null)        store.setEmail(storeDto.email());
        if (storeDto.openingHours() != null) store.setOpeningHours(storeDto.openingHours());
        if (storeDto.latitude() != null)     store.setLatitude(storeDto.latitude());
        if (storeDto.longitude() != null)    store.setLongitude(storeDto.longitude());
        if (storeDto.isActive() != null)     store.setIsActive(storeDto.isActive());
    }

    @Override
    @Transactional
    public void deleteStore(UUID storeId) throws NotFoundException {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        store.setIsActive(false);
        store.setStatus(StoreStatus.CLOSED);

        // ✅ SUPPRIMÉ : store.setUpdatedAt(LocalDateTime.now())

        storeRepository.save(store);
    }
}