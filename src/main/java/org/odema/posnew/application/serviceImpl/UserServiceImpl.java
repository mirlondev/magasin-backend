package org.odema.posnew.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Override
    public UserDto createUser(UserDto userDto) {
        return null;
    }

    @Override
    public UserDto updateUser(UserDto userDto) {
        return null;
    }

    @Override
    public UserDto getUserById(UUID id) {
        return null;
    }

    @Override
    public void deleteUserById(UUID id) {

    }

    @Override
    public List<UserDto> getAllUsers(int page, int size, String sort) {
        return List.of();
    }
}
