package com.mase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import com.mase.dto.auth.AuthResponse;
import com.mase.dto.auth.LoginRequest;
import com.mase.model.Role;
import com.mase.model.User;
import com.mase.repository.UserRepository;
import com.mase.security.JwtService;

@ExtendWith(MockitoExtension.class)
// Unit tests for AuthService authentication and logout.
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @InjectMocks
    private AuthService authService;

    @Test
    // Verifies successful authentication for admin users.
    void authenticate_returnsTokenForAdmin() {
        LoginRequest request = new LoginRequest("AdminUser", "password");
        User user = new User("adminuser", "admin@example.com", "password", Role.ADMIN);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(new UsernamePasswordAuthenticationToken("adminuser", "password"));
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token");
        when(jwtService.getExpirationMillis()).thenReturn(3600_000L);

        AuthResponse response = authService.authenticate(request);

        assertEquals("token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals("adminuser", response.username());
        assertEquals("admin@example.com", response.email());
        assertEquals(Role.ADMIN, response.role());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("adminuser", captor.getValue().getPrincipal());
    }

    @Test
    // Verifies invalid credentials return unauthorized.
    void authenticate_rejectsInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin", "bad");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.authenticate(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    // Verifies non-admin users are rejected.
    void authenticate_rejectsNonAdminUser() {
        LoginRequest request = new LoginRequest("admin", "password");
        User user = new User();
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setPassword("password");
        user.setRole(null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(new UsernamePasswordAuthenticationToken("admin", "password"));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,() -> authService.authenticate(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
    }

    @Test
    // Verifies blank username is rejected.
    void authenticate_rejectsMissingUsername() {
        LoginRequest request = new LoginRequest("  ", "password");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.authenticate(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Username is required", ex.getReason());
    }

    @Test
    // Verifies blank password is rejected.
    void authenticate_rejectsMissingPassword() {
        LoginRequest request = new LoginRequest("admin", "  ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,() -> authService.authenticate(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Password is required", ex.getReason());
    }

    @Test
    // Verifies logout revokes the token.
    void logout_revokesToken() {
        authService.logout("Bearer token-123");

        verify(tokenRevocationService).revoke("token-123");
    }

    @Test
    // Verifies malformed authorization header is rejected.
    void logout_rejectsInvalidHeader() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,() -> authService.logout("Token abc"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Unauthorized", ex.getReason());
    }

    @Test
    // Verifies blank bearer token is rejected.
    void logout_rejectsBlankToken() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,() -> authService.logout("Bearer   "));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Unauthorized", ex.getReason());
    }
}
