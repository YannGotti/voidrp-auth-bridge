package ru.voidrp.authbridge.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.voidrp.authbridge.VoidRpAuthBridge;
import ru.voidrp.authbridge.network.AuthStatusPayload;

public final class ClientAuthStatusHandler {

    private ClientAuthStatusHandler() {
    }

    public static void handle(AuthStatusPayload payload, IPayloadContext context) {
        VoidRpAuthBridge.LOGGER.info(
                "Auth status from server: status={} message={}",
                payload.status(),
                payload.message()
        );
    }
}
