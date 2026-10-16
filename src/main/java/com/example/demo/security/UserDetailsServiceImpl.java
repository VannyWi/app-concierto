package com.example.demo.security;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(UserService.normalizeUsername(username))
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
