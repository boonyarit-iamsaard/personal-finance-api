package me.boonyarit.finance.service;

import lombok.RequiredArgsConstructor;
import me.boonyarit.finance.dto.request.AuthenticationRequest;
import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.entity.RefreshTokenEntity;
import me.boonyarit.finance.entity.UserEntity;
import me.boonyarit.finance.exception.UserAlreadyExistsException;
import me.boonyarit.finance.mapper.UserMapper;
import me.boonyarit.finance.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        UserEntity user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        String token = jwtService.generateToken(user, user.getProvider());
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user);

        return userMapper.toAuthenticationResponse(user, token, refreshToken.getToken());
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
            )
        );

        UserEntity user = (UserEntity) authentication.getPrincipal();

        String token = jwtService.generateToken(user, user.getProvider());
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user);

        return userMapper.toAuthenticationResponse(user, token, refreshToken.getToken());
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        RefreshTokenEntity token = refreshTokenService.refreshToken(refreshToken);
        UserEntity user = token.getUser();

        String newToken = jwtService.generateToken(user, user.getProvider());

        return userMapper.toAuthenticationResponse(user, newToken, token.getToken());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeTokenByString(refreshToken);
    }
}
