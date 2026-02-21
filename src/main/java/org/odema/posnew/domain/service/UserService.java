package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.request.UserRequest;
import org.odema.posnew.application.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse updateUser(UserRequest userDto);

    UserResponse createUser(UserRequest userDto);


    UserResponse getUserById(UUID id);
    void deleteUserById(UUID id);
    List<UserResponse> getAllUsers(int page, int size, String sort);
}
