package org.odema.posnew.mapper;
import org.odema.posnew.entity.Store;
import org.odema.posnew.entity.User;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;



import org.odema.posnew.dto.request.EmployeeRequest;
import org.odema.posnew.dto.request.EmployeeUpdateRequest;
import org.odema.posnew.dto.response.EmployeeResponse;

@Component
public class EmployeeMapper {

    public User toEntity(EmployeeRequest request, Store store) {
        if (request == null) return null;

        return User.builder()
                .username(request.username())
                .password(request.password()) // À encoder dans le service
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .userRole(request.role())
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
                user.getAssignedStore() != null ?
                        UUID.fromString(String.valueOf(user.getAssignedStore().getStoreId())) : null,
                user.getAssignedStore() != null ? user.getAssignedStore().getName() : null,
                user.getAssignedStore() != null ?
                        user.getAssignedStore().getStoreType().name() : null,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLogin()
        );
    }

    public void updateEntityFromRequest(User user, EmployeeUpdateRequest request, Store store) {
        if (request == null) return;

        if (request.username() != null) user.setUsername(request.username());
        if (request.email() != null) user.setEmail(request.email());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.address() != null) user.setAddress(request.address());
        if (request.role() != null) user.setUserRole(request.role());
        if (request.active() != null) user.setActive(request.active());
        if (store != null) user.setAssignedStore(store);
    }
}
