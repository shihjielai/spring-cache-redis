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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
            throw new ErrorCodeException("??????????????????aaaaa", ErrorCodeEnum.USER_NOT_FOUND);
        }

        User user = optionalUser.get();
        return AutoUserMapper.autoUserMapper.mapToUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() throws JsonProcessingException {

        /**
         * ???????????????: ??????????????????
         * ??????????????????: ??????????????????
         * ??????: ??????????????????
         */

        // ???????????????json??????[????????????????????????]

        // ??????????????????????????????json
        String allUsers = stringRedisTemplate.opsForValue().get("allUsers");

        if (StringUtils.isEmpty(allUsers)) {
            System.out.println("??????????????????????????????");
            List<UserDto> allUsersFromDB = getAllUsersFromDB();

            return allUsersFromDB;
        }

        System.out.println("????????????????????????");
        List<UserDto> userDtos = mapper.readValue(allUsers, new TypeReference<List<UserDto>>() {
        });

        return userDtos;
    }

    public List<UserDto> getAllUsersFromDBWithLocalLock() throws JsonProcessingException {

        // ?????????????????????????????????????????????????????????????????????
        // 1. synchronized (this): SpringBoot?????????component??????single instance

        synchronized (this) {

            // ?????????????????????????????????????????????????????????????????????????????????????????????
            return getUsersDtosFromDB();
        }

    }

    public List<UserDto> getAllUsersFromDBWithRedisLock() throws JsonProcessingException {

        // ???????????????(???redis)
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);

        if (lock) {
            // ???????????? ????????????
            // ?????????????????????????????????????????????????????????
            //stringRedisTemplate.expire("lock", 30, TimeUnit.SECONDS);

            List<UserDto> usersDtosFromDB;

            try {
                usersDtosFromDB = getUsersDtosFromDB();
            } finally {
                // ?????????
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";

                Long lock1 = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"),
                        uuid);
            }

            //stringRedisTemplate.delete("lock"); //  ?????????

            // ??????????????? + ?????????????????? = ???????????? lua????????????

//            String lockValue = stringRedisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)) {
//                // ??????????????????
//                stringRedisTemplate.delete("lock");
//            }
            return usersDtosFromDB;
        } else {
            // ????????????...??????
            return getAllUsersFromDBWithRedisLock();
        }
    }

    private List<UserDto> getUsersDtosFromDB() throws JsonProcessingException {
        String allUsers = stringRedisTemplate.opsForValue().get("allUsers");
        if (StringUtils.isNotBlank(allUsers)) {
            // ???????????? null ????????????
            List<UserDto> userDtos = mapper.readValue(allUsers, new TypeReference<List<UserDto>>() {
            });

            return userDtos;
        }

        System.out.println("???????????????...");
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

        // ??????????????????????????????????????????json????????????
        String jsonString = mapper.writeValueAsString(userDtos);
        stringRedisTemplate.opsForValue().set("allUsers", jsonString);
        return userDtos;
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
