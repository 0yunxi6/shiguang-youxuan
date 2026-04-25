package com.ecommerce.service;

import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        String role = StringUtils.hasText(user.getRole()) ? user.getRole().trim().toUpperCase() : "USER";
        boolean enabled = user.getStatus() == null || user.getStatus() == 1;
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            enabled,
            true,
            true,
            true,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
