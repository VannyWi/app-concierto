package com.example.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return isValidPassword(password);
    }

    public static boolean isValidPassword(String password) {
        return password == null || PASSWORD_PATTERN.matcher(password).matches();
    }
}
