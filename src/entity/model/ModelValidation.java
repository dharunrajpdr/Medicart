package entity.model;

import java.math.BigDecimal;
import java.util.Objects;

final class ModelValidation {
    private ModelValidation() {
    }

    static int nonNegativeId(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }

    static int positiveId(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    static String requiredText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " cannot be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain 1 to "
                    + maxLength + " characters");
        }
        return normalized;
    }

    static BigDecimal nonNegativeAmount(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
        return value;
    }
}
