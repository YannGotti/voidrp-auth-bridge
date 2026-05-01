package ru.voidrp.authbridge.server;

import java.time.Instant;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.voidrp.authbridge.VoidRpAuthBridge;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;
import ru.voidrp.authbridge.server.AuthCommandBridge;

public final class ServerAuthHooks {

    private static final long RECONNECT_GRANT_SECONDS = 300L;

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

            // Only give a reconnect grant to legacy-auth accounts (mustUseLauncher=false).
            // Launcher-only accounts must always start a fresh session via the VoidRP launcher.
            if (record.source() == AuthSource.LAUNCHER_TICKET && record.legacyAuthEnabled()) {
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
                VoidRpAuthBridge.LOGGER.info(
                        "No reconnect grant saved for player={} uuid={} (mustUseLauncher=true or legacy login)",
                        record.playerName(),
                        record.playerUuid()
                );
            }
        }

        AuthCommandBridge.clearCooldown(playerUuid);
        ModBootstrap.get().authRestrictionBridge().onPlayerSessionEnded(playerUuid);
        stateStore.clear(playerUuid);
    }
}
