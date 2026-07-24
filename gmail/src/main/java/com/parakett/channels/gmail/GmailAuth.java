package com.parakett.channels.gmail;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parakett.channels.base.CommChannelException;
import com.parakett.channels.base.JsonUtils;
import com.parakett.channels.base.LocalKeyManager;

public class GmailAuth {

    private static final HttpClient _mHttpClient = HttpClient.newHttpClient();

    public static GmailAuth INSTANCE = new GmailAuth();

    private String _mAccessToken = null;

    private JsonNode _mClientCreds = null;
    private ObjectNode _mAccessCreds = null;
    private String _mStorageKey = null;
    private long _mExpiry = -1;

    private GmailAuth() {

    }

    public String getAccessToken() throws CommChannelException {
        if (_mExpiry + 60_000 < System.currentTimeMillis()) {
            // Access token expired.
            refreshToken();
        }

        return _mAccessToken;
    }

    public void initialize(JsonNode accessTokens, JsonNode clientCreds, String storageKey) throws CommChannelException {
        // Check if access token is valid. We ill need to renew 1 minute before expiry.
        _mClientCreds = clientCreds;
        _mAccessCreds = (ObjectNode) (accessTokens.deepCopy()); // Deep copy because we will be modifying
        _mStorageKey = storageKey;

        long expiry = accessTokens.get("expiry").asLong();
        _mExpiry = expiry;

        if (expiry + 60_000 < System.currentTimeMillis()) {
            // Access token expired.
            refreshToken();
        }

        _mAccessToken = _mAccessCreds.get("access_token").asText();
    }

    private void refreshToken() throws CommChannelException {
        String clientId = null;
        String clientSecret = null;

        if (_mClientCreds != null) {
            clientId = _mClientCreds.get("clientId").asText();
            clientSecret = _mClientCreds.get("clientSecret").asText();
        }

        if (clientId == null) {
            throw new CommChannelException(clientSecret);
        }

        String refreshToken = _mAccessCreds.get("refresh_token").asText();

        Map<String, String> formData = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "refresh_token", refreshToken,
                "grant_type", "refresh_token");

        String formBody = formData.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        try {
            HttpResponse<String> response2 = _mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response2.statusCode() != 200) {
                throw new CommChannelException("Google auth request returned status code " + response2.statusCode());
            }

            ObjectNode data = (ObjectNode) JsonUtils.MAPPER.readTree(response2.body());
            _mAccessCreds.put("access_token", data.get("access_token").asText());

            long tms = System.currentTimeMillis() + (3600 * 1000);
            this._mExpiry = tms;
            _mAccessCreds.put("expiry", tms);
            if (_mStorageKey != null) {
                LocalKeyManager lkm = new LocalKeyManager();
                lkm.writeToKeys(_mStorageKey, _mAccessCreds.toString());
            }

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            throw new CommChannelException(e);
        }

    }

}
