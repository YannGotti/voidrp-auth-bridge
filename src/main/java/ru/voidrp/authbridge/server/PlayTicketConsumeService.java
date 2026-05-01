package ru.voidrp.authbridge.server;

import java.time.Instant;
import java.util.UUID;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketRequest;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketResponse;

public final class PlayTicketConsumeService {
    private final BackendAuthClient backendAuthClient;
    private final AuthenticationStateStore stateStore;

    public PlayTicketConsumeService(BackendAuthClient backendAuthClient, AuthenticationStateStore stateStore) {
        this.backendAuthClient = backendAuthClient;
        this.stateStore = stateStore;
    }

    public ConsumePlayTicketResponse authenticate(UUID playerUuid, String playerName, ConsumePlayTicketRequest request) {
        ConsumePlayTicketResponse response = backendAuthClient.consumePlayTicket(request);
        if (response != null && response.accepted() && response.userId() != null) {
            stateStore.markAuthenticated(new AuthenticatedPlayerRecord(
                    playerUuid,
                    response.userId(),
                    playerName,
                    Instant.now(),
                    AuthSource.LAUNCHER_TICKET,
                    response.legacyAuthEnabled()
            ));
        }
        return response;
    }
}