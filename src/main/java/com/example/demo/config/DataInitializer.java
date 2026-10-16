package com.example.demo.config;

import com.example.demo.entity.Role;
import com.example.demo.entity.RoleEntity;
import com.example.demo.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (Role role : Role.values()) {
            if (roleRepository.findByRoleName(role.name()).isEmpty()) {
                RoleEntity entity = new RoleEntity();
                entity.setRoleName(role.name());
                roleRepository.save(entity);
            }
        }
    }
}
