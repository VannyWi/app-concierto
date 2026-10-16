package com.example.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.RoleEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UserDetailsServiceUnitTest {

    @Test
    void loadsNormalizedUsernameWithItsRole() {
        UserRepository repository = mock(UserRepository.class);
        UserEntity user = new UserEntity();
        RoleEntity role = new RoleEntity();
        role.setRoleName("ADMIN");
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setRole(role);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));

        var details = new UserDetailsServiceImpl(repository).loadUserByUsername(" ADMIN ");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");
    }

    @Test
    void rejectsUnknownUsers() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UserDetailsServiceImpl(repository).loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
