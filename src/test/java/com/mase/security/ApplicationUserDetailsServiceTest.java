package com.mase.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.mase.model.Role;
import com.mase.model.User;
import com.mase.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
// Unit tests for ApplicationUserDetailsService.
class ApplicationUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationUserDetailsService service;

    @Test
    // Verifies user lookup returns ApplicationUserDetails.
    void loadUserByUsername_returnsUserDetails() {
        User user = new User("admin", "admin@example.com", "pass", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        assertInstanceOf(ApplicationUserDetails.class, details);
    }

    @Test
    // Verifies missing user throws UsernameNotFoundException.
    void loadUserByUsername_throwsWhenMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }
}
