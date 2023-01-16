package com.example.springbootrestapi.service;

import com.example.springbootrestapi.dto.UserDto;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface UserService {
    UserDto createUser(UserDto user);

    UserDto getUserById(Long userId);

    List<UserDto> getAllUsers() throws JsonProcessingException;

    UserDto updateUser(UserDto user);

    void deleteUser(Long userId);
}
