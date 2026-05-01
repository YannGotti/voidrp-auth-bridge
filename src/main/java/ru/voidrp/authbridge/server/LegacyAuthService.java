package ru.voidrp.authbridge.server;

import java.time.Instant;
import java.util.UUID;
import ru.voidrp.authbridge.common.dto.LegacyLoginRequest;
import ru.voidrp.authbridge.common.dto.LegacyLoginResponse;

public final class LegacyAuthService {
    private final BackendAuthClient backendAuthClient;
    private final AuthenticationStateStore stateStore;

    public LegacyAuthService(BackendAuthClient backendAuthClient, AuthenticationStateStore stateStore) {
        this.backendAuthClient = backendAuthClient;
        this.stateStore = stateStore;
    }

    public LegacyLoginResponse login(UUID playerUuid, String playerName, String password) {
        LegacyLoginResponse response = backendAuthClient.legacyLogin(
                new LegacyLoginRequest(playerName, password)
        );

        if (response != null && response.accepted() && response.userId() != null) {
            stateStore.markAuthenticated(new AuthenticatedPlayerRecord(
                    playerUuid,
                    response.userId(),
                    playerName,
                    Instant.now(),
                    AuthSource.LEGACY_LOGIN,
                    true
            ));
        }

        return response;
    }
}