package ru.voidrp.authbridge.server;

import java.time.Instant;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.voidrp.authbridge.VoidRpAuthBridge;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;

public final class ServerAuthHooks {

    private static final long RECONNECT_GRANT_SECONDS = 7200L;

    private ServerAuthHooks() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var stateStore = ModBootstrap.get().stateStore();
        var player = event.getEntity();
        var playerUuid = player.getUUID();

        var authenticated = stateStore.find(playerUuid);
        if (authenticated.isPresent()) {
            var record = authenticated.get();

            if (record.source() == AuthSource.LAUNCHER_TICKET) {
                Instant expiresAtUtc = Instant.now().plusSeconds(RECONNECT_GRANT_SECONDS);
                stateStore.rememberReconnectGrant(record, expiresAtUtc);

                VoidRpAuthBridge.LOGGER.info(
                        "Saved reconnect grant for player={} uuid={} until={}",
                        record.playerName(),
                        record.playerUuid(),
                        expiresAtUtc
                );
            } else {
                stateStore.removeReconnectGrant(playerUuid);
            }
        }

        ModBootstrap.get().authRestrictionBridge().onPlayerSessionEnded(playerUuid);
        stateStore.clear(playerUuid);
    }
}
