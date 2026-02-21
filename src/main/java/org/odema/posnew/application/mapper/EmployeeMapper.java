package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.EmployeeResponse;
import org.odema.posnew.application.dto.request.EmployeeRequest;
import org.odema.posnew.application.dto.request.EmployeeUpdateRequest;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmployeeMapper {

    public User toEntity(EmployeeRequest request, Store store) {
        if (request == null) return null;

        return User.builder()
                .username(request.username())
                .password(request.password())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .userRole(request.role())
                .assignedStore(store)
                .active(true)
                .build();
    }

    public EmployeeResponse toResponse(User user) {
        if (user == null) return null;

        return new EmployeeResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getActive(),
                user.getUserRole(),
                user.getAssignedStore() != null ? user.getAssignedStore().getStoreId() : null,
                user.getAssignedStore() != null ? user.getAssignedStore().getName() : null,
                user.getAssignedStore() != null && user.getAssignedStore().getStoreType() != null
                        ? user.getAssignedStore().getStoreType().name() : null,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLogin()
        );
    }

    public void updateEntityFromRequest(User user, EmployeeUpdateRequest request, Store store) {
        if (request == null || user == null) return;

        if (request.username() != null) user.setUsername(request.username());
        if (request.email() != null) user.setEmail(request.email());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.address() != null) user.setAddress(request.address());
        if (request.role() != null) user.setUserRole(request.role());
        if (request.active() != null) user.setActive(request.active());
        if (store != null) user.setAssignedStore(store);
    }

    public List<EmployeeResponse> toResponseList(List<User> users) {
        if (users == null) return List.of();
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}