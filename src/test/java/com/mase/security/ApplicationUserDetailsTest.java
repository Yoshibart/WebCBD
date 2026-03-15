package com.mase.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.mase.model.Role;
import com.mase.model.User;

// Unit tests for ApplicationUserDetails.
class ApplicationUserDetailsTest {

    @Test
    // Verifies delegated user fields and role authority mapping.
    void exposesUserFieldsAndAuthorities() {
        User user = new User("admin", "admin@example.com", "secret", Role.ADMIN);
        ApplicationUserDetails details = new ApplicationUserDetails(user);

        assertEquals("admin", details.getUsername());
        assertEquals("secret", details.getPassword());
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }
}
