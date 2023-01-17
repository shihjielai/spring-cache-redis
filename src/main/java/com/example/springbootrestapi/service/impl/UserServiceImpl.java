package com.example.springbootrestapi.service.impl;

import com.example.springbootrestapi.dto.UserDto;
import com.example.springbootrestapi.entity.User;
import com.example.springbootrestapi.errorHandle.enums.ErrorCodeEnum;
import com.example.springbootrestapi.errorHandle.exception.ErrorCodeException;
import com.example.springbootrestapi.mapper.AutoUserMapper;
import com.example.springbootrestapi.mapper.UserMapper;
import com.example.springbootrestapi.repository.UserRepository;
import com.example.springbootrestapi.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final ModelMapper modelMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper mapper;

    @Override
    public UserDto createUser(UserDto userDto) {
        // convert UserDto into User JPA Entity
        //User user = userMapper.mapToUser(userDto);
//        User user = modelMapper.map(userDto, User.class);

        User user = AutoUserMapper.autoUserMapper.mapToUser(userDto);

        User savedUser = userRepository.save(user);

        // Convert User JPA Entity to UserDto
        //UserDto savedUserDto = userMapper.mapToUserDto(savedUser);
//        UserDto savedUserDto = modelMapper.map(savedUser, UserDto.class);
        UserDto savedUserDto = AutoUserMapper.autoUserMapper.mapToUserDto(savedUser);

        return savedUserDto;
    }

    @Override
    public UserDto getUserById(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

//        return userMapper.mapToUserDto(user);
//        return modelMapper.map(user, UserDto.class);

        if (optionalUser.isEmpty()) {
            throw new ErrorCodeException("查無此使用者aaaaa", ErrorCodeEnum.USER_NOT_FOUND);
        }

        User user = optionalUser.get();
        return AutoUserMapper.autoUserMapper.mapToUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() throws JsonProcessingException {

        /**
         * 空結果快取: 解決快取穿透
         * 設定過期時間: 解決快取雪崩
         * 加鎖: 解決快取擊穿
         */

        // 給快取中放json字串[序列化與反序列化]

        // 加入快取邏輯，資料為json
        String allUsers = stringRedisTemplate.opsForValue().get("allUsers");

        if (StringUtils.isEmpty(allUsers)) {
            List<UserDto> allUsersFromDB = getAllUsersFromDB();
            String jsonString = mapper.writeValueAsString(allUsersFromDB);
            stringRedisTemplate.opsForValue().set("allUsers", jsonString);
            return allUsersFromDB;
        }

        List<UserDto> userDtos = mapper.readValue(allUsers, new TypeReference<List<UserDto>>() {});

        return userDtos;
    }

    public List<UserDto> getAllUsersFromDB() throws JsonProcessingException {

        // 只要是同一把鎖，就能鎖住需要這個鎖的所有執行續
        // 1. synchronized (this): SpringBoot所有的component都是single instance

        synchronized (this) {

            // 得到鎖以後，應該再去快取中確認是否存在，如果沒有才需要繼續查詢
            String allUsers = stringRedisTemplate.opsForValue().get("allUsers");
            if (StringUtils.isNotBlank(allUsers)) {
                // 快取不為 null 直接回傳
                List<UserDto> userDtos = mapper.readValue(allUsers, new TypeReference<List<UserDto>>() {});

                return userDtos;
            }

            System.out.println("getAllUsersFromDB");
            List<User> users = userRepository.findAll();
//        List<UserDto> userDtos = users.stream()
//                .map(user -> userMapper.mapToUserDto(user))
//                .collect(Collectors.toList());
//        List<UserDto> userDtos = users.stream()
//                        .map(user -> modelMapper.map(user, UserDto.class))
//                        .collect(Collectors.toList());

            List<UserDto> userDtos = users.stream()
                    .map(user -> AutoUserMapper.autoUserMapper.mapToUserDto(user))
                    .collect(Collectors.toList());

            return userDtos;
        }

    }

    @Override
    public UserDto updateUser(UserDto user) {
        User existingUser = userRepository.findById(user.getId()).get();
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        User updatedUser = userRepository.save(existingUser);
//        return userMapper.mapToUserDto(updatedUser);
//        return modelMapper.map(updatedUser, UserDto.class);
        return AutoUserMapper.autoUserMapper.mapToUserDto(updatedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
