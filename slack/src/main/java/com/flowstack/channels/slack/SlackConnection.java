package com.flowstack.channels.slack;

import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.element.BlockElements;
import com.slack.api.model.event.MessageEvent;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowstack.channels.base.CommChannelException;
import com.flowstack.channels.base.JsonUtils;
import com.flowstack.channels.base.LocalKeyManager;
import com.slack.api.bolt.App;

import com.slack.api.model.block.composition.BlockCompositions;

public class SlackConnection {

    private MessageReceiveHandler _mMessageHandler = null;
    private App _mApp = null;

    private static final String ACTION_APPROVE = "ACTION_APPROVE";
    private static final String ACTION_DENY = "ACTION_DENY";

    SlackConnection() {

    }

    public void setMessageReceiveHandler(MessageReceiveHandler handler) {
        _mMessageHandler = handler;
    }

    public void initialize() throws CommChannelException {
        String botToken = null;
        String appToken = null;

        LocalKeyManager lm = new LocalKeyManager();
        try {
            String k = lm.getKeyValue("slack.channel.config");
            if (k == null) {
                throw new CommChannelException("Key not found.");
            }
            // TODO: Validate key values as well.
            ObjectNode on = (ObjectNode) JsonUtils.MAPPER.readTree(k);
            botToken = on.get("botToken").asText();
            appToken = on.get("appToken").asText();
        } catch (IOException e) {
            throw new CommChannelException("Error loading the keys");
        }

        AppConfig appConfig = AppConfig.builder()
                .singleTeamBotToken(botToken)
                .build();

        App app = new App(appConfig);
        _mApp = app;

        app.event(MessageEvent.class, (payload, ctx) -> {
            MessageEvent event = payload.getEvent();

            // Ignore messages sent by the bot itself to prevent infinite loops
            if (event.getBotId() != null) {
                return ctx.ack();
            }

            // Extract message details
            String text = event.getText();
            String channelId = event.getChannel();
            String userId = event.getUser();

            String threadId = event.getThreadTs() != null ? event.getThreadTs() : event.getTs();

            if (_mMessageHandler != null) {
                _mMessageHandler.onMessageReceived(text, userId, channelId, threadId);
            }

            return ctx.ack(); // Acknowledge the event back to Slack
        });

        try {

            SocketModeApp socketModeApp = new SocketModeApp(appToken, app);
            System.out.println("Slack Bot is starting in Socket Mode...");

            Thread.startVirtualThread(() -> {
                try {
                    socketModeApp.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });

        } catch (IOException e) {
            throw new CommChannelException(e);
        }
    }

    public void sendMessage(String channelId, String text, String threadId) {
        if (this._mApp == null) {
            System.err.println("Slack App is not initialized yet.");
            return;
        }

        // Use virtual threads (matching your startup style) to keep API calls
        // non-blocking
        Thread.startVirtualThread(() -> {
            try {
                // Get the Slack API client
                MethodsClient client = this._mApp.getClient();

                // Build the message request
                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                        .channel(channelId)
                        .text(text)
                        .threadTs(threadId) // <--- This targets the specific thread!
                        .build();

                ChatPostMessageResponse response = client.chatPostMessage(request);

                if (!response.isOk()) {
                    System.err.println("Failed to send message: " + response.getError());
                }
            } catch (Exception e) {
                System.err.println("Error sending message to Slack:");
                e.printStackTrace();
            }
        });
    }

    public void triggerBlockForGettingResponse(final String channeld, final String message, String requestId,
            final String responseKey) {
        _mApp.executorService().submit(() -> {
            try {
                _mApp.getClient().chatPostMessage(r -> r
                        .token(_mApp.config().getSingleTeamBotToken())
                        .channel(channeld)
                        .text("Approval required for account action.")

                        // --- EXPLICIT BUILDERS TO AVOID COMPILER CONFUSION ---
                        .blocks(Blocks.asBlocks(

                                Blocks.header(
                                        h -> h.text(BlockCompositions.plainText("🤖 System Action Pending Request"))),

                                Blocks.divider(),

                                Blocks.section(s -> s.text(BlockCompositions.markdownText(
                                        String.format("⚠️ *Attention:*\n\n*" + message + "*")))),

                                Blocks.actions(a -> a
                                        .elements(BlockElements.asElements(

                                                // Green Button
                                                BlockElements.button(b -> b
                                                        .actionId(ACTION_APPROVE)
                                                        .text(BlockCompositions.plainText("Approve Request"))
                                                        .style("primary")
                                                        .value(requestId)),

                                                // Red Button
                                                BlockElements.button(b -> b
                                                        .actionId(ACTION_DENY)
                                                        .text(BlockCompositions.plainText("Cancel Request"))
                                                        .style("danger")
                                                        .value(requestId)))))

                        )));

                _mApp.blockAction("ACTION_APPROVE", (req, ctx) -> {
                    // Extract the value you passed (requestId)
                    String actionRequestId = req.getPayload().getActions().get(0).getValue();

                    // Get the user who clicked it
                    String userId = req.getPayload().getUser().getId();
                    String threadId = null;
                    if (req.getPayload().getContainer() != null) {
                        threadId = req.getPayload().getContainer().getThreadTs();
                    }

                    // TODO: Your business logic (e.g., update database, approve request)
                    sendConfirmationResponseToHandler(userId, channeld, threadId, actionRequestId, message, responseKey,
                            true);

                    try {
                         //Show the block, without buttons.
                        ctx.respond(res -> res
                                .replaceOriginal(true) // This tells Slack to overwrite the existing message
                                .blocks(Blocks.asBlocks(
                                        Blocks.header(
                                                h -> h.text(BlockCompositions.plainText("✅ System Action Approved"))),
                                        Blocks.divider(),
                                        Blocks.section(s -> s.text(BlockCompositions.markdownText(
                                                String.format("~*Attention:*~\n\n~*" + message
                                                        + "*~\n\n🟢 *Approved by <@%s>*", userId))))

                        )));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Acknowledge the request back to Slack (200 OK)
                    return ctx.ack();
                });

                // 2. Handle the Cancel Button Click
                _mApp.blockAction("ACTION_DENY", (req, ctx) -> {
                    String actionRequestId = req.getPayload().getActions().get(0).getValue();
                    String userId = req.getPayload().getUser().getId();
                    String threadId = null;
                    if (req.getPayload().getContainer() != null) {
                        threadId = req.getPayload().getContainer().getThreadTs();
                    }

                    // TODO: Your business logic (e.g., deny request)
                    System.out.println("Request " + requestId + " denied by " + userId);
                    sendConfirmationResponseToHandler(userId, channeld, threadId, actionRequestId, message, responseKey,
                            false);

                    try {
                        //Show the block, without buttons.
                        ctx.respond(res -> res
                                .replaceOriginal(true) // This tells Slack to overwrite the existing message
                                .blocks(Blocks.asBlocks(
                                        Blocks.header(
                                                h -> h.text(BlockCompositions.plainText("✅ System Action Approved"))),
                                        Blocks.divider(),
                                        Blocks.section(s -> s.text(BlockCompositions.markdownText(
                                                String.format("~*Attention:*~\n\n~*" + message
                                                        + "*~\n\n🟢 *Approved by <@%s>*", userId))))
                        )));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return ctx.ack();
                });

            } catch (Exception e) {
                System.err.println("CRITICAL: Failed to push block layout to Slack API: " + e.getMessage());
            }
        });
    }

    private void sendConfirmationResponseToHandler(String userId,
            String channelId, String threadId, String requestId,
            String message, String responseKey, boolean confirmation) {

        if (_mMessageHandler != null) {
            ObjectNode msg = JsonUtils.MAPPER.createObjectNode();
            ObjectNode r = JsonUtils.MAPPER.createObjectNode();
            msg.set("content", r);
            msg.put("type", "hilConfirmationResponse");
            r.put("message", message);
            r.put(responseKey, confirmation);
            r.put(requestId, requestId);
            _mMessageHandler.onMessageReceived(msg.toString(), userId, channelId, threadId);
        }
    }

}
