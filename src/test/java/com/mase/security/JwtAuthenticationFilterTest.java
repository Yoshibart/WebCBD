package com.mase.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.mase.service.TokenRevocationService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
// Unit tests for JwtAuthenticationFilter behavior.
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    // Verifies unauthenticated requests pass through unchanged.
    void doFilter_skipsWhenNoAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    // Verifies revoked tokens do not authenticate.
    void doFilter_skipsWhenTokenRevoked() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenRevocationService.isRevoked("revoked")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    // Verifies valid tokens populate the SecurityContext.
    void doFilter_setsAuthenticationWhenTokenValid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenRevocationService.isRevoked("valid")).thenReturn(false);
        when(jwtService.extractUsername("valid")).thenReturn("user");
        UserDetails userDetails = new User(
                "user",
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    // Verifies parsing errors clear the SecurityContext.
    void doFilter_clearsContextOnJwtException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenRevocationService.isRevoked("broken")).thenReturn(false);
        when(jwtService.extractUsername("broken")).thenThrow(new JwtException("bad token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
