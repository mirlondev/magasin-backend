package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.request.UserRequest;
import org.odema.posnew.application.dto.response.UserResponse;
import org.odema.posnew.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        if (request == null) return null;

        return User.builder()
                .username(request.username())
                .password(request.password())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .userRole(request.userRole())
                .active(true)
                .build();
    }

    public UserResponse toResponse(User user) {
        if (user == null) return null;

        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getActive(),
                user.getUserRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLogin()
        );
    }

    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null) return List.of();
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntityFromRequest(User user, UserRequest request) {
        if (request == null || user == null) return;

        if (request.username() != null) user.setUsername(request.username());
        if (request.email() != null) user.setEmail(request.email());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.address() != null) user.setAddress(request.address());
        if (request.userRole() != null) user.setUserRole(request.userRole());
        // Password is handled separately
    }
}