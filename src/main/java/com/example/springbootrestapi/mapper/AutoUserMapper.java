package com.example.springbootrestapi.mapper;

import com.example.springbootrestapi.dto.UserDto;
import com.example.springbootrestapi.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AutoUserMapper {

    AutoUserMapper autoUserMapper = Mappers.getMapper(AutoUserMapper.class);

    UserDto mapToUserDto(User user);

    User mapToUser(UserDto userDto);
}
