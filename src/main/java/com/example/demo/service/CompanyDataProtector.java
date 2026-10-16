package com.example.demo.service;

import com.example.demo.entity.CompanyEntity;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CompanyDataProtector {

    private static final byte[] RUC_LOOKUP_CONTEXT = "company-ruc-lookup-v1".getBytes(StandardCharsets.UTF_8);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKey encryptionKey;
    private final SecretKey lookupKey;

    public CompanyDataProtector(@Value("${app.company.encryption-key}") String encodedKey) {
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) throw new IllegalArgumentException("must contain 32 bytes");
            encryptionKey = new SecretKeySpec(key, "AES");
            lookupKey = new SecretKeySpec(hmac(key, RUC_LOOKUP_CONTEXT), "HmacSHA256");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_COMPANY_ENCRYPTION_KEY must be a Base64-encoded 32-byte key", exception);
        }
    }

    public String encrypt(String value) {
        if (value == null) return null;
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt company data", exception);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted);
            if (payload.length <= 12) throw new IllegalArgumentException("invalid encrypted payload");
            byte[] iv = new byte[12];
            byte[] ciphertext = new byte[payload.length - iv.length];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt company data", exception);
        }
    }

    public String rucHash(String ruc) {
        return hex(hmac(lookupKey.getEncoded(), ruc.getBytes(StandardCharsets.UTF_8)));
    }

    public CompanyData decrypt(CompanyEntity company) {
        return new CompanyData(decrypt(company.getNameEncrypted()), decrypt(company.getRucEncrypted()), decrypt(company.getAddressEncrypted()));
    }

    private byte[] hmac(byte[] key, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to derive company lookup key", exception);
        }
    }

    private String hex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) result.append(String.format("%02x", item));
        return result.toString();
    }

    public record CompanyData(String name, String ruc, String address) { }
}
