package com.example.demo.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(100)
@RequiredArgsConstructor
public class CompanyEncryptionMigration implements ApplicationRunner {

    private final CompanyDataProtector protector;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!hasLegacyColumns()) return;
        List<Map<String, Object>> companies = jdbc.queryForList("SELECT id, name, ruc, address FROM companies WHERE name_encrypted IS NULL OR ruc_encrypted IS NULL");
        for (Map<String, Object> company : companies) {
            String name = (String) company.get("name");
            String ruc = (String) company.get("ruc");
            String address = (String) company.get("address");
            if (name == null || ruc == null) throw new IllegalStateException("Company migration found incomplete data");
            jdbc.update("UPDATE companies SET name_encrypted = ?, ruc_encrypted = ?, address_encrypted = ?, ruc_hash = ? WHERE id = ?", protector.encrypt(name), protector.encrypt(ruc), protector.encrypt(address), protector.rucHash(ruc), company.get("id"));
        }
        jdbc.execute("ALTER TABLE companies DROP COLUMN name");
        jdbc.execute("ALTER TABLE companies DROP COLUMN ruc");
        jdbc.execute("ALTER TABLE companies DROP COLUMN address");
    }

    private boolean hasLegacyColumns() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE lower(table_name) = 'companies' AND lower(column_name) IN ('name', 'ruc', 'address')", Integer.class);
        return count != null && count == 3;
    }
}
