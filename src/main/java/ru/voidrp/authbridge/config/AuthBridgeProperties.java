package ru.voidrp.authbridge.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

public record AuthBridgeProperties(
        URI backendBaseUrl,
        Duration requestTimeout,
        Path localTicketPath,
        String consumeTicketPath,
        String legacyLoginPath,
        String gameAuthSecret
) {
    public static AuthBridgeProperties loadDefault() {
        String baseUrl = System.getProperty("voidrp.auth.backend", "https://api.void-rp.ru");
        String timeoutMs = System.getProperty("voidrp.auth.timeoutMs", "5000");
        String ticketPath = System.getProperty("voidrp.auth.ticketPath", defaultTicketPath().toString());
        String gameSecret = System.getProperty("voidrp.auth.gameSecret", "");

        return new AuthBridgeProperties(
                URI.create(baseUrl),
                Duration.ofMillis(Long.parseLong(timeoutMs)),
                Path.of(ticketPath),
                "/api/v1/server/auth/consume-play-ticket",
                "/api/v1/server/auth/legacy-login",
                gameSecret
        );
    }

    private static Path defaultTicketPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "VoidRpLauncher", "state", "play-ticket.json");
        }

        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, "AppData", "Local", "VoidRpLauncher", "state", "play-ticket.json");
    }
}