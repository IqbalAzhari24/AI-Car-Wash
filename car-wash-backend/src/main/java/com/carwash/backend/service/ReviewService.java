package com.carwash.backend.service;

import com.carwash.backend.dto.ReviewDto;
import com.carwash.backend.entity.Booking;
import com.carwash.backend.entity.Review;
import com.carwash.backend.repository.BookingRepository;
import com.carwash.backend.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final SentimentAnalysisService sentimentAnalysisService;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         SentimentAnalysisService sentimentAnalysisService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    /** Returns the existing review for a booking, or empty if none yet. */
    public Optional<ReviewDto> findByBooking(UUID bookingId, UUID callerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!booking.getCustomer().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return reviewRepository.findByBooking_Id(bookingId).map(ReviewDto::from);
    }

    @Transactional
    public ReviewDto submit(UUID bookingId, UUID callerId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be 1–5.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!booking.getCustomer().getId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Reviews are only allowed after the wash is completed.");
        }
        if (reviewRepository.findByBooking_Id(bookingId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already reviewed.");
        }

        Review r = new Review();
        r.setBooking(booking);
        r.setCustomer(booking.getCustomer());
        r.setRating(rating);
        r.setComment(comment);
        r.setSentimentScore(sentimentAnalysisService.score(rating, comment));

        return ReviewDto.from(reviewRepository.save(r));
    }
}
