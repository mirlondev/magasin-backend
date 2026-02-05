package org.odema.posnew.service;

import org.odema.posnew.dto.UserDto;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDto createUser(UserDto userDto);
    UserDto updateUser(UserDto userDto);
    UserDto getUserById(UUID id);
    void deleteUserById(UUID id);
    List<UserDto> getAllUsers(int page, int size, String sort);
}
