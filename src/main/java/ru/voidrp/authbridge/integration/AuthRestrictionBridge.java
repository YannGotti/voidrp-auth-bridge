package ru.voidrp.authbridge.integration;

import java.util.UUID;

public interface AuthRestrictionBridge {
    void onPlayerAuthenticated(UUID playerUuid);

    static AuthRestrictionBridge noop() {
        return playerUuid -> {
            // Intentionally blank.
            // Replace with a bridge into your existing server-side action limiter.
        };
    }
}
