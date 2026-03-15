package com.mase.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mase.dto.auth.AuthResponse;
import com.mase.dto.auth.LoginRequest;
import com.mase.model.Role;
import com.mase.service.AuthService;

@ExtendWith(MockitoExtension.class)
// Unit tests for AuthController request delegation.
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    // Verifies login delegates to the service and returns its response.
    void login_delegatesToService() {
        LoginRequest request = new LoginRequest("admin", "password");
        AuthResponse response = new AuthResponse("token", "Bearer", 3600, "admin", "admin@example.com", Role.ADMIN);
        when(authService.authenticate(request)).thenReturn(response);

        AuthResponse result = controller.login(request);

        assertEquals(response, result);
    }

    @Test
    // Verifies logout delegates token revocation to the service.
    void logout_delegatesToService() {
        controller.logout("Bearer token");

        verify(authService).logout("Bearer token");
    }
}
