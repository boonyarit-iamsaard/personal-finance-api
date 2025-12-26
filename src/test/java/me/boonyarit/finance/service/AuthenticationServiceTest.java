package me.boonyarit.finance.service;

import me.boonyarit.finance.dto.request.AuthenticationRequest;
import me.boonyarit.finance.dto.request.RegisterRequest;
import me.boonyarit.finance.dto.response.AuthenticationResponse;
import me.boonyarit.finance.entity.RefreshTokenEntity;
import me.boonyarit.finance.entity.UserEntity;
import me.boonyarit.finance.enumeration.AuthProvider;
import me.boonyarit.finance.enumeration.Role;
import me.boonyarit.finance.exception.UserAlreadyExistsException;
import me.boonyarit.finance.mapper.UserMapper;
import me.boonyarit.finance.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";
    private static final String TEST_ENCODED_PASSWORD = "encoded_SecurePassword123";
    private static final String TEST_INVALID_PASSWORD = "invalid_SecurePassword123";
    private static final String TEST_TOKEN = "TestJwtToken";
    private static final String TEST_REFRESH_TOKEN = "TestRefreshToken";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Register: Should save user with encoded password when email is unique")
    void register_ShouldCreateUserAndReturnAuthenticationResponse_WhenEmailIsUnique() {
        RegisterRequest request = createRegisterRequest();
        UserEntity rawUser = createRawUserEntity();
        AuthenticationResponse expectedAuthenticationResponse = createAuthenticationResponse();

        when(userRepository.existsByEmailIgnoreCase(TEST_EMAIL)).thenReturn(false);
        when(userRepository.save(rawUser)).thenReturn(rawUser);

        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
        when(jwtService.generateToken(rawUser, AuthProvider.LOCAL)).thenReturn(TEST_TOKEN);

        RefreshTokenEntity refreshToken = createRefreshTokenEntity(rawUser);
        when(refreshTokenService.createRefreshToken(rawUser)).thenReturn(refreshToken);

        when(userMapper.toEntity(request)).thenReturn(rawUser);
        when(userMapper.toAuthenticationResponse(rawUser, TEST_TOKEN, TEST_REFRESH_TOKEN)).thenReturn(expectedAuthenticationResponse);

        AuthenticationResponse actualAuthenticationResponse = authenticationService.register(request);

        assertNotNull(actualAuthenticationResponse);
        assertEquals(TEST_TOKEN, actualAuthenticationResponse.token());

        // We capture the user entity passed to save method to ensure the service
        // actually performed the side effect of encoding the password before saving.
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity savedUser = captor.getValue();
        assertEquals(TEST_ENCODED_PASSWORD, savedUser.getPassword(), "Service should have encoded the password before saving");
        assertEquals(TEST_EMAIL, savedUser.getEmail());
    }

    @Test
    @DisplayName("Register: Should throw exception when email already exists")
    void register_ShouldThrowUserAlreadyExistsException_WhenEmailIsAlreadyExists() {
        RegisterRequest request = createRegisterRequest();
        when(userRepository.existsByEmailIgnoreCase(TEST_EMAIL)).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> authenticationService.register(request)
        );

        assertEquals("User with email " + TEST_EMAIL + " already exists", exception.getMessage());
        verify(userRepository).existsByEmailIgnoreCase(TEST_EMAIL);
        verifyNoInteractions(userMapper, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    @DisplayName("Authenticate: Should return token when credentials are valid")
    void authenticate_ShouldReturnAuthenticationResponse_WhenCredentialsAreValid() {
        AuthenticationRequest request = new AuthenticationRequest(TEST_EMAIL, TEST_PASSWORD);
        UserEntity authenticatedUser = createRawUserEntity();
        authenticatedUser.setPassword(TEST_ENCODED_PASSWORD);

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getPrincipal()).thenReturn(authenticatedUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authenticationMock);
        when(jwtService.generateToken(authenticatedUser, AuthProvider.LOCAL))
            .thenReturn(TEST_TOKEN);

        RefreshTokenEntity refreshToken = createRefreshTokenEntity(authenticatedUser);
        when(refreshTokenService.createRefreshToken(authenticatedUser)).thenReturn(refreshToken);

        when(userMapper.toAuthenticationResponse(authenticatedUser, TEST_TOKEN, TEST_REFRESH_TOKEN))
            .thenReturn(createAuthenticationResponse());

        AuthenticationResponse actualAuthenticationResponse = authenticationService.authenticate(request);

        assertNotNull(actualAuthenticationResponse);
        assertEquals(TEST_TOKEN, actualAuthenticationResponse.token());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());

        UsernamePasswordAuthenticationToken authenticationToken = captor.getValue();
        assertEquals(TEST_EMAIL, authenticationToken.getPrincipal());
        assertEquals(TEST_PASSWORD, authenticationToken.getCredentials());
    }

    @Test
    @DisplayName("Authenticate: Should throw exception when credentials are invalid")
    void authenticate_ShouldThrowAuthenticationException_WhenCredentialsAreInvalid() {
        AuthenticationRequest request = new AuthenticationRequest(TEST_EMAIL, TEST_INVALID_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));

        verifyNoInteractions(jwtService, userMapper);
    }

    private RegisterRequest createRegisterRequest() {
        return new RegisterRequest(
            TEST_FIRST_NAME,
            TEST_LAST_NAME,
            TEST_EMAIL,
            TEST_PASSWORD
        );
    }

    private UserEntity createRawUserEntity() {
        return UserEntity.builder()
            .firstName(TEST_FIRST_NAME)
            .lastName(TEST_LAST_NAME)
            .email(TEST_EMAIL)
            .password(TEST_PASSWORD)
            .role(Role.USER)
            .provider(AuthProvider.LOCAL)
            .build();
    }

    private AuthenticationResponse createAuthenticationResponse() {
        return new AuthenticationResponse(
            TEST_TOKEN,
            TEST_REFRESH_TOKEN,
            TEST_EMAIL,
            TEST_FIRST_NAME,
            TEST_LAST_NAME,
            AuthProvider.LOCAL.name()
        );
    }

    private RefreshTokenEntity createRefreshTokenEntity(UserEntity user) {
        return RefreshTokenEntity.builder()
            .token(TEST_REFRESH_TOKEN)
            .user(user)
            .build();
    }
}
