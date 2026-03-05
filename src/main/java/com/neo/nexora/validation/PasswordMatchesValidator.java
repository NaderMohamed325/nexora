package com.neo.nexora.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * Validator implementation for {@link PasswordMatches} annotation.
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordFieldName;
    private String confirmPasswordFieldName;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.passwordFieldName = constraintAnnotation.password();
        this.confirmPasswordFieldName = constraintAnnotation.confirmPassword();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field passwordField = value.getClass().getDeclaredField(passwordFieldName);
            Field confirmPasswordField = value.getClass().getDeclaredField(confirmPasswordFieldName);

            passwordField.setAccessible(true);
            confirmPasswordField.setAccessible(true);

            Object password = passwordField.get(value);
            Object confirmPassword = confirmPasswordField.get(value);

            if (password == null && confirmPassword == null) {
                return true;
            }

            return password != null && password.equals(confirmPassword);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }
}

