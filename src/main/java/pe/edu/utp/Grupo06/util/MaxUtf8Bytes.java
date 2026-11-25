package pe.edu.utp.Grupo06.util;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;

/**
 * Valida que una cadena no supere un límite máximo de bytes en UTF-8
 * (por ejemplo, 72 bytes para evitar el truncamiento de BCrypt).
 */
@Documented
@Constraint(validatedBy = MaxUtf8Bytes.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8Bytes {
    int value() default 72;
    String message() default "Supera el límite máximo de bytes UTF-8 permitidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<MaxUtf8Bytes, String> {
        private int maxBytes;

        @Override
        public void initialize(MaxUtf8Bytes constraintAnnotation) {
            this.maxBytes = constraintAnnotation.value();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            return value.getBytes(StandardCharsets.UTF_8).length <= maxBytes;
        }
    }
}
