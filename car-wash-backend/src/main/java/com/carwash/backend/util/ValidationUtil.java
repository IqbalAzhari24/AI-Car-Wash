package com.carwash.backend.util;

import java.util.regex.Pattern;

/**
 * Shared input-validation helpers for Malaysian phone numbers and email addresses.
 *
 * <p>Phone numbers must be in international format starting with {@code +60}:
 * mobile ({@code +601X} followed by 7–8 digits) or landline ({@code +60} area
 * code 3–9 followed by 7–8 digits).
 */
public final class ValidationUtil {

    /** +60 mobile (1x, 8–9 remaining digits) or landline (area 3–9, 7–8 remaining digits). */
    private static final Pattern MY_PHONE = Pattern.compile("^\\+60(1\\d{8,9}|[3-9]\\d{7,8})$");

    /** Pragmatic RFC-style email check: local@domain.tld with a 2+ letter TLD. */
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtil() {}

    /**
     * Normalizes a raw phone input: strips spaces/dashes/parentheses and converts
     * local prefixes ({@code 60...}, {@code 0...}) to the {@code +60} form.
     *
     * @param raw user-supplied phone number, may be null/blank
     * @return normalized phone string, or the original null/blank value
     */
    public static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String p = raw.replaceAll("[\\s\\-()]", "");
        if (p.startsWith("60")) p = "+" + p;
        else if (p.startsWith("0")) p = "+60" + p.substring(1);
        return p;
    }

    /**
     * @param phone a phone number (ideally already passed through {@link #normalizePhone})
     * @return true if it is a valid Malaysian number in +60 format
     */
    public static boolean isValidMalaysianPhone(String phone) {
        return phone != null && MY_PHONE.matcher(phone).matches();
    }

    /**
     * @param email candidate email address
     * @return true if it follows the standard local@domain.tld format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }
}
