package com.example.demo.config;

import com.example.demo.entity.Role;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import com.example.demo.validation.PasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class BootstrapAdminConfiguration {

    @Bean
    @Order(2)
    CommandLineRunner bootstrapAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username}") String configuredUsername,
            @Value("${app.bootstrap-admin.password}") String password) {
        return arguments -> {
            if (configuredUsername.isBlank() || password.isBlank()) {
                return;
            }
            String username = UserService.normalizeUsername(configuredUsername);
            if (userRepository.existsByUsername(username)) {
                return;
            }
            if (!PasswordValidator.isValidPassword(password)) {
                throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD does not meet the password policy");
            }

            UserEntity admin = new UserEntity();
            admin.setUsername(username);
            admin.setFirstName("Administrador");
            admin.setLastName("Sistema");
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(roleRepository.findByRoleName(Role.ADMIN.name())
                    .orElseThrow(() -> new IllegalStateException("ADMIN role was not initialized")));
            userRepository.save(admin);
        };
    }
}
