package com.ecommerce.service;

import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setRole("USER");
        when(userMapper.findByUsername("testuser")).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userMapper).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_nonExistingUser_throwsException() {
        when(userMapper.findByUsername("unknown")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown"));
    }
}
