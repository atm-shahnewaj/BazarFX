package com.bazarfx.service;

import com.bazarfx.model.Message;
import com.bazarfx.storage.FileStorageManager;
import com.bazarfx.util.IdGenerator;
import com.bazarfx.util.InputValidator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple buyer <-> seller chat, scoped to a single product. A "conversation" is the
 * (productId, buyerId, sellerId) triple - whichever account is logged in (buyer or
 * seller) can open the same thread and reply, which matches switching accounts to
 * answer as the seller.
 */
public class MessagingService {

    private final FileStorageManager storage;

    public MessagingService(FileStorageManager storage) {
        this.storage = storage;
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }

    public Message send(String productId, String buyerId, String sellerId, String senderId, String body) {
        if (!InputValidator.isNonEmpty(body)) throw new ValidationException("Message can't be empty.");
        Message message = new Message(IdGenerator.newId(), productId, buyerId, sellerId, senderId, body.trim());
        storage.addMessage(message);
        return message;
    }

    /** All messages for one conversation, oldest first. */
    public List<Message> conversation(String productId, String buyerId, String sellerId) {
        return storage.messagesSnapshot().stream()
                .filter(m -> m.getProductId().equals(productId) && m.getBuyerId().equals(buyerId) && m.getSellerId().equals(sellerId))
                .sorted(Comparator.comparingLong(Message::getSentAt))
                .collect(Collectors.toList());
    }

    /** One representative (most recent) message per conversation this user is part of, newest first. */
    public List<Message> conversationsFor(String userId) {
        Map<String, Message> latestByConversation = new LinkedHashMap<>();
        List<Message> all = storage.messagesSnapshot().stream()
                .filter(m -> m.getBuyerId().equals(userId) || m.getSellerId().equals(userId))
                .sorted(Comparator.comparingLong(Message::getSentAt))
                .collect(Collectors.toList());
        for (Message m : all) {
            latestByConversation.put(m.conversationKey(), m);
        }
        List<Message> result = new ArrayList<>(latestByConversation.values());
        result.sort((a, b) -> Long.compare(b.getSentAt(), a.getSentAt()));
        return result;
    }
}
