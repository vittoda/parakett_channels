package com.flowstack.channels.gmail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flowstack.channels.base.CommChannelException;
import com.flowstack.channels.base.CommChannelInstance;
import com.flowstack.channels.base.JsonUtils;
import com.flowstack.channels.base.LocalKeyManager;
import com.flowstack.channels.base.MessageContext;
import com.flowstack.channels.base.OnMessageHandler;
import com.flowstack.channels.base.OutputMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GmailChannelInstance implements CommChannelInstance {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmailChannelInstance.class);

    private final HttpClient _mClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService _mScheduler = Executors.newScheduledThreadPool(1);
    private OnMessageHandler _mMessageHandler = null;

    private JsonNode _mConfig = null;
    private MessageFilter _mFilter = null;

    GmailChannelInstance(JsonNode config) {
        _mConfig = config;
    }

    @Override
    public void sendMessage(OutputMessage msg) throws CommChannelException {
        // TODO:Ignore for now.
    }

    @Override
    public void registerOnMessageHandler(OnMessageHandler handler) {
        _mMessageHandler = handler;
    }

    @Override
    public void initialize() throws CommChannelException {
        try {
            LOGGER.info("Initializing Gmail channel");

            JsonNode accessTokens = null;
            JsonNode clientCreds = null;

            String storageKey = null;

            if (_mConfig != null) {
                if (_mConfig.has("credentials") && !_mConfig.get("credentials").isNull()) {
                    JsonNode credentials = _mConfig.get("credentials");

                    accessTokens = credentials.get("accessTokens");
                    clientCreds = credentials.get("clientCreds");
                } else {
                    LocalKeyManager lm = new LocalKeyManager();

                    // Get access tokens from local key manager
                    String accessTokenKey = null;
                    if (_mConfig.has("accessTokenKey") && !_mConfig.get("accessTokenKey").isNull()) {
                        accessTokenKey = _mConfig.get("accessTokenKey").asText();
                    } else {
                        accessTokenKey = "google.gmail.tokens";
                    }

                    storageKey = accessTokenKey;

                    String accessTokensString = lm.getKeyValue(accessTokenKey);
                    if (accessTokensString != null) {
                        accessTokens = JsonUtils.MAPPER.readTree(accessTokensString);
                    }

                    // Get client credentials from local key manager
                    String clientCredsKey = null;
                    if (_mConfig.has("clientCredsKey") && !_mConfig.get("clientCredsKey").isNull()) {
                        clientCredsKey = _mConfig.get("clientCredsKey").asText();
                    } else {
                        clientCredsKey = "google.gmail.clientCreds";
                    }

                    String clientCredsString = lm.getKeyValue(clientCredsKey);
                    if (clientCredsString != null) {
                        clientCreds = JsonUtils.MAPPER.readTree(clientCredsString);
                    }
                }

            }

            if (accessTokens == null) {
                throw new CommChannelException(
                        "Access tokens could not be retreieved. Check your channel instance configuration");
            }

            GmailAuth.INSTANCE.initialize(accessTokens, clientCreds, storageKey);

            // Run the thread.
            LOGGER.info("Starting the email polling thread.");
            _mScheduler.scheduleAtFixedRate(this::queryEmail, 0, 1, TimeUnit.MINUTES);

            if (_mConfig != null && _mConfig.has("filter")) {
                _mFilter = MessageFilter.fromJSON(_mConfig.get("filter"));
            }

        } catch (IOException e) {
            throw new CommChannelException(e);
        }
    }

    @Override
    public void getConfirmationResponse(MessageContext context, String message, String respnseKey, String requestId)
            throws CommChannelException {
        throw new CommChannelException("GMaail channel does not support sending messages as of now.");
    }

    private void queryEmail() {
        LOGGER.info("Checking email.");
        // TODO: Ignore it when there are not handlers registered.
        try {
            long oneMinuteAgo = (System.currentTimeMillis() / 1000) - 600;
            String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=after:" + oneMinuteAgo
                    + "%20label:inbox";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + GmailAuth.INSTANCE.getAccessToken())
                    .GET()
                    .build();

            HttpResponse<String> response = _mClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CommChannelException("Error in getting the emails. HTTP API responded with status code "
                        + response.statusCode() + ". Expected 200. \n" + response.body());
            }
            JsonNode on = JsonUtils.MAPPER.readTree(response.body());
            ArrayNode messages = (ArrayNode) on.get("messages");
            if (messages != null && !messages.isNull()) {
                LOGGER.info("Found {} emails.", messages.size());
                for (JsonNode messageNode : messages) {
                    try {
                        // TODO: Use batch fetch as needed.
                        Thread.sleep(1000); // 1 Second delay to avoid throttling.
                    } catch (InterruptedException e) {
                    }
                    String messageId = messageNode.get("id").asText();
                    try {
                        GmailInputMessage msg = getMatchingMessage(messageId);
                        if (msg == null) {
                            continue;
                        }
                        _mMessageHandler.onMessageReceived(msg);
                    } catch (CommChannelException e) {
                        e.printStackTrace();
                        LOGGER.error("Error triggering onMessage handler.", e);
                    }
                }
            }

        } catch (InterruptedException | IOException | CommChannelException e) {
            e.printStackTrace();
        }
    }

    private GmailInputMessage getMatchingMessage(String messageId) throws CommChannelException {
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId + "?format=full";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + GmailAuth.INSTANCE.getAccessToken())
                    .GET()
                    .build();

            HttpResponse<String> response = _mClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CommChannelException("Error in getting email details. HTTP API respnded with statis code "
                        + response.statusCode() + ". Expected 200.\n" + response.body());
            }
            JsonNode on = JsonUtils.MAPPER.readTree(response.body());

            ArrayNode headers = (ArrayNode) on.get("payload").get("headers");
            String senderEmail = null, threadId = null, subject = null;

            for (int i = 0; i < headers.size(); i++) {
                JsonNode header = headers.get(i);
                String headerName = header.get("name").asText();
                if (headerName.equals("From")) {
                    senderEmail = header.get("value").asText();
                } else if (headerName.equals("Subject")) {
                    subject = header.get("value").asText();
                }
            }

            JsonNode payload = on.get("payload");
            String fullBodyText = getTextFromBodyOrParts(payload);
            if (fullBodyText == null || fullBodyText.isEmpty()) {
                fullBodyText = on.has("snippet") ? on.get("snippet").asText() : "";
            }

            LOGGER.info("Email from '{}'", senderEmail);
            // Check if it matches
            if (shouldSendTheMessage(subject, senderEmail, fullBodyText)) {
                GmailMessageContext ctx = new GmailMessageContext(subject, messageId, senderEmail, threadId);
                return new GmailInputMessage(fullBodyText, ctx);
            }

            return null;
        } catch (InterruptedException | IOException e) {
            throw new CommChannelException("Error in getting the emails. Exception message : " + e.getMessage());
        }
    }

    private boolean shouldSendTheMessage(String subject, String sender, String body) {
        if (_mFilter == null) {
            return true;
        }

        return _mFilter.matches(subject, sender, body);
    }

    private String getTextFromBodyOrParts(JsonNode node) {
        // Check if this specific part contains text
        JsonNode body = node.get("body");
        if (body != null && body.has("data")) {
            String mimeType = node.has("mimeType") ? node.get("mimeType").asText() : "";
            if ("text/plain".equals(mimeType) || "text/html".equals(mimeType)) {
                String base64Data = body.get("data").asText();
                // Decode Base64URL
                byte[] decodedBytes = Base64.getUrlDecoder().decode(base64Data);
                String rawText = new String(decodedBytes, StandardCharsets.UTF_8);

                // If it's HTML, we'll flag it or handle stripping later,
                // but prefer text/plain if both exist. We look for text/plain first.
                return mimeType + ":::" + rawText;
            }
        }

        // If it has nested parts, traverse them recursively
        if (node.has("parts")) {
            ArrayNode parts = (ArrayNode) node.get("parts");
            String htmlFallback = null;

            for (JsonNode part : parts) {
                String result = getTextFromBodyOrParts(part);
                if (result != null) {
                    if (result.startsWith("text/plain:::")) {
                        return result.substring("text/plain:::".length()); // Found ideal plain text!
                    } else if (result.startsWith("text/html:::")) {
                        htmlFallback = result.substring("text/html:::".length());
                    }
                }
            }
            // If we didn't find plain text but found HTML, strip HTML and return it
            if (htmlFallback != null) {
                return stripHtml(htmlFallback);
            }
        }
        return null;
    }

    private String stripHtml(String html) {
        if (html == null)
            return "";
        // Replaces HTML tags, breaks, and converts common entities
        String cleanText = html.replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");
        // Collapse multiple spaces/newlines into clean spacing
        return cleanText.replaceAll("\\s+", " ").trim();
    }

    private static class MessageFilter {

        String subjectContains = null;
        String subjectStarts = null;
        String subjectEnds = null;

        String senderContains = null;
        String senderStarts = null;
        String senderEnds = null;

        String bodyContains = null;

        private MessageFilter() {

        }

        public boolean matches(String subject, String sender, String body) {

            // For now, every condition should match
            if (subjectContains != null) {
                if (subject == null || !subject.contains(subjectContains)) {
                    return false;
                }
            }

            if (subjectStarts != null) {
                if (subject == null || !subject.startsWith(subjectStarts)) {
                    return false;
                }
            }

            if (subjectEnds != null) {
                if (subject == null || !subject.endsWith(subjectEnds)) {
                    return false;
                }
            }

            if (senderContains != null) {
                if (sender == null || !sender.contains(senderContains)) {
                    return false;
                }
            }

            if (senderStarts != null) {
                if (sender == null || !sender.startsWith(senderStarts)) {
                    return false;
                }
            }

            if (senderEnds != null) {
                if (sender == null || !sender.endsWith(senderEnds)) {
                    return false;
                }
            }

            if (bodyContains != null) {
                if (body == null || !body.contains(bodyContains)) {
                    return false;
                }
            }

            return true;
        }

        public static MessageFilter fromJSON(JsonNode filterNode) {
            MessageFilter filter = new MessageFilter();

            if (filterNode.has("subject")) {
                JsonNode subjectFilterNode = filterNode.get("subject");
                if (subjectFilterNode.has("contains")) {
                    filter.subjectContains = subjectFilterNode.get("contains").asText();
                }
                if (subjectFilterNode.has("startsWith")) {
                    filter.subjectStarts = subjectFilterNode.get("startsWith").asText();
                }
                if (subjectFilterNode.has("endsWidth")) {
                    filter.subjectEnds = subjectFilterNode.get("endsWidth").asText();
                }
            }

            if (filterNode.has("sender")) {
                JsonNode senderFilterNode = filterNode.get("sender");
                if (senderFilterNode.has("contains")) {
                    filter.senderContains = senderFilterNode.get("contains").asText();
                }
                if (senderFilterNode.has("startsWith")) {
                    filter.senderStarts = senderFilterNode.get("startsWith").asText();
                }
                if (senderFilterNode.has("endsWidth")) {
                    filter.senderEnds = senderFilterNode.get("endsWidth").asText();
                }
            }

            if (filterNode.has("body")) {
                JsonNode bodyFilterNode = filterNode.get("body");
                if (bodyFilterNode.has("contains")) {
                    filter.bodyContains = bodyFilterNode.get("body").asText();
                }
            }

            return filter;
        }
    }

}
