package me.boonyarit.finance.mapper;

import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.dto.response.UserResponse;
import me.boonyarit.finance.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "provider", constant = "LOCAL")
    UserEntity toEntity(RegisterRequest registerRequest);

    @Mapping(target = "token", source = "token")
    @Mapping(target = "refreshToken", source = "refreshToken")
    AuthenticationResponse toAuthenticationResponse(UserEntity user, String token, String refreshToken);

    UserResponse toUserResponse(UserEntity user);
}
