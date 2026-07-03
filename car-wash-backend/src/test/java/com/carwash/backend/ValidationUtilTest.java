package com.carwash.backend;

import com.carwash.backend.util.ValidationUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void acceptsValidMalaysianNumbers() {
        assertTrue(ValidationUtil.isValidMalaysianPhone("+60123456789"));    // mobile 012
        assertTrue(ValidationUtil.isValidMalaysianPhone("+60169220499"));    // mobile 016
        assertTrue(ValidationUtil.isValidMalaysianPhone("+601112345678"));   // mobile 011 (10 digits)
        assertTrue(ValidationUtil.isValidMalaysianPhone("+60322821234"));    // KL landline
        assertTrue(ValidationUtil.isValidMalaysianPhone("+6096221234"));     // Terengganu landline
    }

    @Test
    void rejectsInvalidNumbers() {
        assertFalse(ValidationUtil.isValidMalaysianPhone("0123456789"));     // missing +60
        assertFalse(ValidationUtil.isValidMalaysianPhone("+65123456789"));   // Singapore
        assertFalse(ValidationUtil.isValidMalaysianPhone("+601234"));        // too short
        assertFalse(ValidationUtil.isValidMalaysianPhone("+6012345678901")); // too long
        assertFalse(ValidationUtil.isValidMalaysianPhone("+60abcdefgh"));
        assertFalse(ValidationUtil.isValidMalaysianPhone(null));
    }

    @Test
    void normalizesLocalFormatsToPlus60() {
        assertEquals("+60123456789", ValidationUtil.normalizePhone("0123456789"));
        assertEquals("+60123456789", ValidationUtil.normalizePhone("012-345 6789"));
        assertEquals("+60169220499", ValidationUtil.normalizePhone("+6016 922 0499"));
        assertEquals("+60123456789", ValidationUtil.normalizePhone("60123456789"));
        assertNull(ValidationUtil.normalizePhone(null));
    }

    @Test
    void validatesEmailFormat() {
        assertTrue(ValidationUtil.isValidEmail("aysiahazhari98@gmail.com"));
        assertTrue(ValidationUtil.isValidEmail("a.b+tag@sub.domain.my"));
        assertFalse(ValidationUtil.isValidEmail("no-at-sign.com"));
        assertFalse(ValidationUtil.isValidEmail("missing@tld"));
        assertFalse(ValidationUtil.isValidEmail("two@@example.com"));
        assertFalse(ValidationUtil.isValidEmail(null));
    }
}
