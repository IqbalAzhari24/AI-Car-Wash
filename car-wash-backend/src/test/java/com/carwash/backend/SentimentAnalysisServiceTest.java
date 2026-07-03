package com.carwash.backend;

import com.carwash.backend.service.SentimentAnalysisService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lexicon-path tests — no Gemini API key is configured in unit tests, so the
 * service falls back to the bilingual keyword lexicon / rating prior.
 */
class SentimentAnalysisServiceTest {

    private final SentimentAnalysisService service = new SentimentAnalysisService();

    @Test
    void positiveEnglishCommentScoresPositive() {
        BigDecimal score = service.score(5, "Excellent service, very clean and the staff were friendly!");
        assertTrue(score.doubleValue() > 0.5, "expected positive, got " + score);
    }

    @Test
    void negativeEnglishCommentScoresNegative() {
        BigDecimal score = service.score(1, "Terrible. Slow service and my car was still dirty.");
        assertTrue(score.doubleValue() < -0.5, "expected negative, got " + score);
    }

    @Test
    void malayCommentsAreScored() {
        assertTrue(service.score(5, "Sangat bagus, kereta bersih dan staf mesra").doubleValue() > 0.5);
        assertTrue(service.score(2, "Lambat sangat dan mahal, kecewa").doubleValue() < -0.5);
    }

    @Test
    void mixedCommentLandsBetweenExtremes() {
        BigDecimal score = service.score(3, "Wash was good but the wait was slow");
        assertTrue(score.doubleValue() >= -0.5 && score.doubleValue() <= 0.5, "expected mixed, got " + score);
    }

    @Test
    void blankCommentFallsBackToRating() {
        assertEquals(new BigDecimal("1.00"), service.score(5, null));
        assertEquals(new BigDecimal("0.00"), service.score(3, "   "));
        assertEquals(new BigDecimal("-1.00"), service.score(1, ""));
    }

    @Test
    void noKeywordMatchFallsBackToRating() {
        BigDecimal score = service.score(4, "Okay lah");
        assertEquals(new BigDecimal("0.50"), score);
    }

    @Test
    void scoreIsAlwaysWithinBounds() {
        for (int rating = 1; rating <= 5; rating++) {
            BigDecimal s = service.score(rating, "best best best worst");
            assertTrue(s.doubleValue() >= -1.0 && s.doubleValue() <= 1.0);
        }
    }
}
