package kz.iitu.springlab.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** Variant 1: an ISBN-10 or ISBN-13 with a valid check digit. A blank value passes. */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsbnValidator.class)
@Documented
public @interface Isbn {
    String message() default "{book.isbn.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
