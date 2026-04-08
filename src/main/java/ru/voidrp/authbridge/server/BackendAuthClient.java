package ru.voidrp.authbridge.server;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketRequest;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketResponse;
import ru.voidrp.authbridge.common.dto.LegacyLoginRequest;
import ru.voidrp.authbridge.common.dto.LegacyLoginResponse;
import ru.voidrp.authbridge.common.json.GsonFactory;
import ru.voidrp.authbridge.common.util.HttpJson;
import ru.voidrp.authbridge.config.AuthBridgeProperties;
import ru.voidrp.authbridge.common.dto.PlayerAccessRequest;
import ru.voidrp.authbridge.common.dto.PlayerAccessResponse;

public final class BackendAuthClient {
    private final AuthBridgeProperties properties;
    private final Gson gson;
    private final HttpClient httpClient;

    public BackendAuthClient(AuthBridgeProperties properties) {
        this.properties = properties;
        this.gson = GsonFactory.create();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.requestTimeout())
                .build();
    }

    public ConsumePlayTicketResponse consumePlayTicket(ConsumePlayTicketRequest request) {
        URI uri = properties.backendBaseUrl().resolve(properties.consumeTicketPath());
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .header("X-Game-Auth-Secret", properties.gameAuthSecret())
                .timeout(properties.requestTimeout())
                .POST(HttpJson.body(gson, request))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return gson.fromJson(response.body(), ConsumePlayTicketResponse.class);
            }
            return ConsumePlayTicketResponse.failed("http_" + response.statusCode() + ": " + response.body());
        } catch (IOException ex) {
            return ConsumePlayTicketResponse.failed("io_error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ConsumePlayTicketResponse.failed("interrupted: " + ex.getMessage());
        } catch (RuntimeException ex) {
            return ConsumePlayTicketResponse.failed("client_error: " + ex.getMessage());
        }
    }

    public LegacyLoginResponse legacyLogin(LegacyLoginRequest request) {
        URI uri = properties.backendBaseUrl().resolve(properties.legacyLoginPath());
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .header("X-Game-Auth-Secret", properties.gameAuthSecret())
                .timeout(properties.requestTimeout())
                .POST(HttpJson.body(gson, request))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return gson.fromJson(response.body(), LegacyLoginResponse.class);
            }
            return LegacyLoginResponse.failed("http_" + response.statusCode() + ": " + response.body());
        } catch (IOException ex) {
            return LegacyLoginResponse.failed("io_error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return LegacyLoginResponse.failed("interrupted: " + ex.getMessage());
        } catch (RuntimeException ex) {
            return LegacyLoginResponse.failed("client_error: " + ex.getMessage());
        }
    }

    public PlayerAccessResponse getPlayerAccess(String playerName) {
        URI uri = properties.backendBaseUrl().resolve("/api/v1/server/auth/player-access");
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/json")
                .header("X-Game-Auth-Secret", properties.gameAuthSecret())
                .timeout(properties.requestTimeout())
                .POST(HttpJson.body(gson, new PlayerAccessRequest(playerName)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return gson.fromJson(response.body(), PlayerAccessResponse.class);
            }
            return PlayerAccessResponse.failed("http_" + response.statusCode() + ": " + response.body());
        } catch (IOException ex) {
            return PlayerAccessResponse.failed("io_error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return PlayerAccessResponse.failed("interrupted: " + ex.getMessage());
        } catch (RuntimeException ex) {
            return PlayerAccessResponse.failed("client_error: " + ex.getMessage());
        }
    }
}