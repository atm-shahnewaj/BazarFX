package com.bazarfx.service;

import com.bazarfx.model.Order;
import com.bazarfx.model.OrderStatus;
import com.bazarfx.model.Review;
import com.bazarfx.model.User;
import com.bazarfx.storage.FileStorageManager;
import com.bazarfx.util.IdGenerator;
import com.bazarfx.util.InputValidator;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewService {

    private final FileStorageManager storage;

    public ReviewService(FileStorageManager storage) {
        this.storage = storage;
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }

    public Review addReview(Order order, int rating, String comment) {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ValidationException("You can only review a seller after the order is delivered.");
        }
        if (!InputValidator.isValidRating(rating)) {
            throw new ValidationException("Rating must be between 1 and 5.");
        }

        Review review = new Review(IdGenerator.newId(), order.getId(), order.getBuyerId(), order.getSellerId(), rating, comment);
        storage.addReview(review);
        recomputeSellerRating(order.getSellerId());
        return review;
    }

    public void flagReview(Review review) {
        review.setFlagged(true);
        storage.updateReview(review);
    }

    public List<Review> reviewsForSeller(String sellerId) {
        return storage.reviewsSnapshot().stream()
                .filter(r -> r.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    public void recomputeSellerRating(String sellerId) {
        List<Review> reviews = reviewsForSeller(sellerId);
        if (reviews.isEmpty()) return;
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);

        User seller = storage.usersMap().get(sellerId);
        if (seller != null) {
            seller.setSellerRatingAvg(avg);
            seller.setSellerRatingCount(reviews.size());
            storage.saveUser(seller);
        }
    }
}
