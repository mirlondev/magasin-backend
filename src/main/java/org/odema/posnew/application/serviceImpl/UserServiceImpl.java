package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.request.UserRequest;
import org.odema.posnew.application.dto.response.UserResponse;
import org.odema.posnew.domain.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Override
    public UserResponse createUser(UserRequest userDto) {
        return null;
    }

    @Override
    public UserResponse updateUser(UserRequest userDto) {
        return null;
    }

    @Override
    public UserResponse getUserById(UUID id) {
        return null;
    }

    @Override
    public void deleteUserById(UUID id) {

    }

    @Override
    public List<UserResponse> getAllUsers(int page, int size, String sort) {
        return List.of();
    }
}
