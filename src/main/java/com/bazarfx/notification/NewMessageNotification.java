package com.bazarfx.notification;

import com.bazarfx.model.Message;

public class NewMessageNotification extends AppNotification {
    private final Message message;
    private final String senderName;

    public NewMessageNotification(Message message, String senderName) {
        this.message = message;
        this.senderName = senderName;
    }

    @Override
    public String getIcon() { return "\uD83D\uDCAC"; }

    @Override
    public String getMessage() {
        String preview = message.getBody().length() > 40 ? message.getBody().substring(0, 40) + "\u2026" : message.getBody();
        return "New message from " + senderName + ": " + preview;
    }

    @Override
    public String getStyleClass() { return "toast-info"; }
}
