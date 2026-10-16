package kz.iitu.springlab.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsbnValidator implements ConstraintValidator<Isbn, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return true;   // the field is optional
        String isbn = value.replace("-", "").replace(" ", "").toUpperCase();
        return switch (isbn.length()) {
            case 10 -> isValidIsbn10(isbn);
            case 13 -> isValidIsbn13(isbn);
            default -> false;                                  // wrong length
        };
    }

    /** ISBN-10: sum of digit * (10..1) must be divisible by 11; the last char may be X = 10. */
    static boolean isValidIsbn10(String isbn) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn.charAt(i);
            int digit;
            if (i == 9 && c == 'X') digit = 10;
            else if (Character.isDigit(c)) digit = c - '0';
            else return false;
            sum += digit * (10 - i);
        }
        return sum % 11 == 0;
    }

    /** ISBN-13: digits weighted 1,3,1,3...; the total must be divisible by 10. */
    static boolean isValidIsbn13(String isbn) {
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            char c = isbn.charAt(i);
            if (!Character.isDigit(c)) return false;
            sum += (c - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return sum % 10 == 0;
    }
}
