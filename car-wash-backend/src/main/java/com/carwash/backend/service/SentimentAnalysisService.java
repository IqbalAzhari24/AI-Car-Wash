package com.carwash.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Sentiment scoring for customer review comments, range -1.00 (very negative)
 * to 1.00 (very positive).
 *
 * <p>Primary scorer is Gemini 2.5 Flash via the raw REST API (same integration
 * style as {@link TimahAiService} — no Spring AI). When the API key is absent
 * or the call fails, a bilingual (English + Bahasa Malaysia) keyword lexicon is
 * used, falling back to a rating-derived score when no keywords match. Scoring
 * must never block or fail a review submission.
 */
@Service
public class SentimentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisService.class);

    private static final String GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ponytail: word-set lexicon, swap for a proper NLP model if FYP scope ever demands it
    private static final Set<String> POSITIVE = Set.of(
            "good", "great", "excellent", "amazing", "awesome", "love", "loved", "best",
            "clean", "fast", "quick", "friendly", "satisfied", "perfect", "recommend",
            "bagus", "cepat", "mesra", "puas", "bersih", "cantik", "terbaik", "mantap", "berbaloi");

    private static final Set<String> NEGATIVE = Set.of(
            "bad", "slow", "dirty", "rude", "poor", "terrible", "worst", "late",
            "expensive", "disappointed", "disappointing", "awful", "horrible", "scratch", "scratched",
            "lambat", "kotor", "mahal", "teruk", "buruk", "kecewa", "hampeh", "calar");

    private final ObjectMapper om = new ObjectMapper();

    @Value("${gemini.api.key:GEMINI_API_KEY_NOT_SET}")
    private String geminiApiKey;

    /**
     * Scores a review's sentiment. Never throws — always returns a value in
     * [-1.00, 1.00], scale 2.
     *
     * @param rating  the 1–5 star rating (used as fallback signal)
     * @param comment free-text comment, may be null/blank
     * @return sentiment score, -1.00 .. 1.00
     */
    public BigDecimal score(int rating, String comment) {
        if (comment == null || comment.isBlank()) {
            return ratingPrior(rating);
        }
        if (geminiApiKey != null && !geminiApiKey.isBlank() && !"GEMINI_API_KEY_NOT_SET".equals(geminiApiKey)) {
            BigDecimal ai = scoreWithGemini(comment);
            if (ai != null) return ai;
        }
        return lexiconScore(comment, rating);
    }

    // -------------------------------------------------------------------------
    // Gemini scorer
    // -------------------------------------------------------------------------

    private BigDecimal scoreWithGemini(String comment) {
        try {
            ObjectNode body = om.createObjectNode();
            ObjectNode content = body.putArray("contents").addObject();
            content.put("role", "user");
            content.putArray("parts").addObject().put("text",
                    "Rate the sentiment of this car-wash customer review (may be English or Bahasa Malaysia). "
                    + "Respond with ONLY a decimal number between -1 (very negative) and 1 (very positive). "
                    + "Review: \"" + comment.replace("\"", "'") + "\"");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GENERATE_URL))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Gemini sentiment call returned HTTP {}", response.statusCode());
                return null;
            }

            JsonNode text = om.readTree(response.body())
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode()) return null;

            double value = Double.parseDouble(text.asText().trim());
            return clamp(value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("Gemini sentiment scoring failed, falling back to lexicon: {}", e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Lexicon fallback
    // -------------------------------------------------------------------------

    private BigDecimal lexiconScore(String comment, int rating) {
        int pos = 0, neg = 0;
        for (String word : comment.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (POSITIVE.contains(word)) pos++;
            else if (NEGATIVE.contains(word)) neg++;
        }
        if (pos + neg == 0) {
            return ratingPrior(rating);
        }
        return clamp((pos - neg) / (double) (pos + neg));
    }

    /** Maps a 1–5 rating onto -1..1 (3 stars = neutral). */
    private BigDecimal ratingPrior(int rating) {
        return clamp((rating - 3) / 2.0);
    }

    private BigDecimal clamp(double value) {
        double bounded = Math.max(-1.0, Math.min(1.0, value));
        return BigDecimal.valueOf(bounded).setScale(2, RoundingMode.HALF_UP);
    }
}
