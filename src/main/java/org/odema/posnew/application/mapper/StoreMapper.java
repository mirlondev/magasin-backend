package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.request.StoreRequest;
import org.odema.posnew.application.dto.response.StoreResponse;
import org.odema.posnew.application.serviceImpl.StoreServiceImpl;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.enums.StoreStatus;
import org.odema.posnew.domain.model.enums.StoreType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StoreMapper {

    public Store toEntity(StoreRequest request) {
        if (request == null) return null;

        return Store.builder()
                .name(request.name())
                .address(request.address())
                .city(request.city())
                .postalCode(request.postalCode())
                .country(request.country())
                .storeType(request.storeType() != null ? request.storeType() : StoreType.SHOP)
                .status(request.status() != null ? request.status() : StoreStatus.PENDING)
                .phone(request.phone())
                .email(request.email())
                .openingHours(request.openingHours())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();
    }

    public StoreResponse toResponse(Store store) {
        if (store == null) return null;

        return new StoreResponse(
                store.getStoreId(),
                store.getName(),
                store.getAddress(),
                store.getCity(),
                store.getPostalCode(),
                store.getCountry(),
                store.getStoreType(),
                store.getStatus(),
                store.getPhone(),
                store.getEmail(),
                store.getOpeningHours(),
                store.getLatitude(),
                store.getLongitude(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                store.getIsActive(),
                store.getStoreAdmin() != null ? store.getStoreAdmin().getUserId() : null
        );
    }

    public List<StoreResponse> toResponseList(List<Store> stores) {
        if (stores == null) return List.of();
        return stores.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntityFromRequest(Store store, StoreRequest request) {
        if (request == null || store == null) return;

        if (request.name() != null) store.setName(request.name());
        if (request.address() != null) store.setAddress(request.address());
        StoreServiceImpl.getDtoData(request, store);
    }
}