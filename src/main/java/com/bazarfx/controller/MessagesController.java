package com.bazarfx.controller;

import com.bazarfx.AppContext;
import com.bazarfx.model.Message;
import com.bazarfx.model.Product;
import com.bazarfx.model.User;
import com.bazarfx.notification.NewMessageNotification;
import com.bazarfx.service.MessagingService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Buyer <-> seller chat screen. The left column lists every conversation the current
 * account is part of (as buyer OR seller); the right column is the open thread. Since
 * this is a single desktop account switching between roles (as the assignment expects:
 * message a seller from a buyer account, then log in as the seller to reply), whichever
 * account is currently logged in simply sees every thread it participates in.
 */
public class MessagesController {

    @FXML private VBox conversationList;
    @FXML private VBox chatBox;
    @FXML private ScrollPane chatScroll;
    @FXML private Label chatHeader;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Label emptyStateLabel;

    private String openProductId;
    private String openBuyerId;
    private String openSellerId;

    @FXML
    public void initialize() {
        sendButton.setDisable(true);
        messageField.setDisable(true);
        refreshConversationList();
    }

    private void refreshConversationList() {
        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        conversationList.getChildren().clear();

        List<Message> conversations = ctx.messagingService.conversationsFor(current.getId());
        if (conversations.isEmpty()) {
            Label empty = new Label("No conversations yet. Message a seller from any listing!");
            empty.getStyleClass().add("page-subtext");
            empty.setWrapText(true);
            conversationList.getChildren().add(empty);
            return;
        }

        for (Message latest : conversations) {
            conversationList.getChildren().add(buildConversationRow(latest, current));
        }
    }

    private HBox buildConversationRow(Message latest, User current) {
        AppContext ctx = AppContext.get();
        boolean iAmBuyer = latest.getBuyerId().equals(current.getId());
        String otherId = iAmBuyer ? latest.getSellerId() : latest.getBuyerId();
        User other = ctx.storage.usersMap().get(otherId);
        Product product = ctx.productService.getById(latest.getProductId());

        VBox textCol = new VBox(2);
        Label who = new Label((other != null ? other.getUsername() : "Unknown") + (iAmBuyer ? " (seller)" : " (buyer)"));
        who.getStyleClass().add("card-title");
        Label about = new Label(product != null ? product.getTitle() : "Listing removed");
        about.getStyleClass().add("card-meta");
        String preview = latest.getBody().length() > 34 ? latest.getBody().substring(0, 34) + "\u2026" : latest.getBody();
        Label lastMsg = new Label(preview);
        lastMsg.getStyleClass().add("detail-meta");
        textCol.getChildren().addAll(who, about, lastMsg);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, textCol, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("conversation-row");
        row.setPadding(new Insets(8));
        row.setOnMouseClicked(e -> openConversation(latest.getProductId(), latest.getBuyerId(), latest.getSellerId()));
        return row;
    }

    /** Opens (or starts) the conversation for one product between one buyer and one seller. */
    public void openConversation(String productId, String buyerId, String sellerId) {
        this.openProductId = productId;
        this.openBuyerId = buyerId;
        this.openSellerId = sellerId;

        AppContext ctx = AppContext.get();
        Product product = ctx.productService.getById(productId);
        User buyer = ctx.storage.usersMap().get(buyerId);
        User seller = ctx.storage.usersMap().get(sellerId);
        String productTitle = product != null ? product.getTitle() : "listing";

        chatHeader.setText("About \u201c" + productTitle + "\u201d \u2014 "
                + (buyer != null ? buyer.getUsername() : "buyer") + " \u2194 "
                + (seller != null ? seller.getUsername() : "seller"));

        sendButton.setDisable(false);
        messageField.setDisable(false);
        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);

        renderMessages();
    }

    private void renderMessages() {
        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        chatBox.getChildren().clear();

        List<Message> messages = ctx.messagingService.conversation(openProductId, openBuyerId, openSellerId);
        for (Message m : messages) {
            boolean mine = m.getSenderId().equals(current.getId());
            Label bubble = new Label(m.getBody());
            bubble.setWrapText(true);
            bubble.setMaxWidth(360);
            bubble.getStyleClass().add(mine ? "chat-bubble-mine" : "chat-bubble-theirs");
            HBox row = new HBox(bubble);
            row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            chatBox.getChildren().add(row);
        }
        javafx.application.Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text == null || text.isBlank() || openProductId == null) return;

        AppContext ctx = AppContext.get();
        User current = ctx.authService.getCurrentUser();
        try {
            Message sent = ctx.messagingService.send(openProductId, openBuyerId, openSellerId, current.getId(), text);
            messageField.clear();
            renderMessages();
            refreshConversationList();

            // Whoever isn't the sender would "receive" this in a networked app; here we
            // simply demo the popup with the sender's own name, matching the same-machine
            // account-switching workflow described for this feature.
            ctx.notificationPoller.publish(new NewMessageNotification(sent, current.getUsername()));
        } catch (MessagingService.ValidationException ex) {
            // Ignore blank sends silently - the field itself prevents most of these.
        }
    }
}
