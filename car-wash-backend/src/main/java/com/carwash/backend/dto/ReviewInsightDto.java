package com.carwash.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Aggregated customer-review sentiment insight for the owner dashboard. */
public class ReviewInsightDto {

    /** A single flagged review (most negative first). */
    public static class FlaggedReview {
        private final Integer rating;
        private final String comment;
        private final BigDecimal sentimentScore;
        private final LocalDateTime createdAt;

        public FlaggedReview(Integer rating, String comment, BigDecimal sentimentScore, LocalDateTime createdAt) {
            this.rating = rating;
            this.comment = comment;
            this.sentimentScore = sentimentScore;
            this.createdAt = createdAt;
        }

        public Integer getRating() { return rating; }
        public String getComment() { return comment; }
        public BigDecimal getSentimentScore() { return sentimentScore; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    private final long totalReviews;
    private final BigDecimal averageRating;
    private final BigDecimal averageSentiment;
    private final long positiveCount;
    private final long neutralCount;
    private final long negativeCount;
    private final String summary;
    private final List<FlaggedReview> mostNegative;

    public ReviewInsightDto(long totalReviews, BigDecimal averageRating, BigDecimal averageSentiment,
                            long positiveCount, long neutralCount, long negativeCount,
                            String summary, List<FlaggedReview> mostNegative) {
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
        this.averageSentiment = averageSentiment;
        this.positiveCount = positiveCount;
        this.neutralCount = neutralCount;
        this.negativeCount = negativeCount;
        this.summary = summary;
        this.mostNegative = mostNegative;
    }

    public long getTotalReviews() { return totalReviews; }
    public BigDecimal getAverageRating() { return averageRating; }
    public BigDecimal getAverageSentiment() { return averageSentiment; }
    public long getPositiveCount() { return positiveCount; }
    public long getNeutralCount() { return neutralCount; }
    public long getNegativeCount() { return negativeCount; }
    public String getSummary() { return summary; }
    public List<FlaggedReview> getMostNegative() { return mostNegative; }
}
