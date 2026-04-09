package ru.voidrp.authbridge.server;

import java.time.Instant;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.voidrp.authbridge.VoidRpAuthBridge;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;
import ru.voidrp.authbridge.common.dto.PlayerAccessResponse;

public final class ServerJoinGateHooks {

    private static final long AUTH_GRACE_SECONDS = 180L;

    private static final Component WAITING_FOR_LAUNCHER = Component.literal(
            "Ожидаем подтверждение входа через лаунчер VoidRP..."
    );
    private static final Component WAITING_FOR_TICKET_OR_LOGIN = Component.literal(
            "Проверяем вход. Если подтверждение из лаунчера не придёт, используй /login <password>."
    );
    private static final Component LEGACY_LOGIN_REQUIRED = Component.literal(
            "Для входа используйте /login <password>"
    );
    private static final Component LAUNCHER_ONLY_KICK = Component.literal(
            "Авторизация доступна только через лаунчер VoidRP."
    );
    private static final Component ACCOUNT_DENIED_KICK = Component.literal(
            "Вход в игру сейчас недоступен для этого аккаунта."
    );
    private static final Component RECONNECT_ACCEPTED = Component.literal(
            "Повторный вход подтверждён. Можно продолжать игру."
    );

    private ServerJoinGateHooks() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerUuid = player.getUUID();
        String playerName = player.getGameProfile().getName();
        var stateStore = ModBootstrap.get().stateStore();

        var reconnectGrant = stateStore.findActiveReconnectGrant(
                playerUuid,
                playerName,
                Instant.now()
        );

        if (reconnectGrant.isPresent()) {
            var grant = reconnectGrant.get();

            stateStore.markAuthenticated(new AuthenticatedPlayerRecord(
                    playerUuid,
                    grant.userId(),
                    playerName,
                    Instant.now(),
                    grant.source()
            ));

            player.sendSystemMessage(RECONNECT_ACCEPTED);

            VoidRpAuthBridge.LOGGER.info(
                    "Player restored from reconnect grant: player={} uuid={} expiresAtUtc={}",
                    playerName,
                    playerUuid,
                    grant.expiresAtUtc()
            );
            return;
        }

        PlayerAccessResponse access = ModBootstrap.get().backendAuthClient().getPlayerAccess(playerName);
        Instant deadline = Instant.now().plusSeconds(AUTH_GRACE_SECONDS);

        stateStore.markPending(
                playerUuid,
                new AuthenticationStateStore.PendingPlayerRecord(
                        deadline,
                        access.legacyAuthEnabled(),
                        access.mustUseLauncher(),
                        player.getX(),
                        player.getY(),
                        player.getZ()
                )
        );

        VoidRpAuthBridge.LOGGER.info(
                "Player entered auth gate: player={} uuid={} legacyEnabled={} mustUseLauncher={} deadlineUtc={} backendError={}",
                playerName,
                playerUuid,
                access.legacyAuthEnabled(),
                access.mustUseLauncher(),
                deadline,
                access.error()
        );

        if (access.legacyAuthEnabled()) {
            player.sendSystemMessage(WAITING_FOR_TICKET_OR_LOGIN);
        } else {
            player.sendSystemMessage(WAITING_FOR_LAUNCHER);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var stateStore = ModBootstrap.get().stateStore();
        var pending = stateStore.snapshotPending();

        for (var entry : pending.entrySet()) {
            UUID playerUuid = entry.getKey();
            AuthenticationStateStore.PendingPlayerRecord record = entry.getValue();

            if (Instant.now().isBefore(record.deadlineUtc())) {
                continue;
            }

            if (stateStore.isAuthenticated(playerUuid)) {
                continue;
            }

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUuid);
            if (player == null) {
                stateStore.clear(playerUuid);
                continue;
            }

            if (record.legacyAuthEnabled()) {
                stateStore.markLegacyPending(playerUuid);
                player.sendSystemMessage(LEGACY_LOGIN_REQUIRED);

                VoidRpAuthBridge.LOGGER.info(
                        "Player moved to legacy pending: player={} uuid={}",
                        player.getGameProfile().getName(),
                        playerUuid
                );
                continue;
            }

            if (record.mustUseLauncher()) {
                VoidRpAuthBridge.LOGGER.warn(
                        "Kicking player because launcher auth did not arrive before deadline: player={} uuid={}",
                        player.getGameProfile().getName(),
                        playerUuid
                );
                player.connection.disconnect(LAUNCHER_ONLY_KICK);
            } else {
                VoidRpAuthBridge.LOGGER.warn(
                        "Kicking player because account access is denied: player={} uuid={}",
                        player.getGameProfile().getName(),
                        playerUuid
                );
                player.connection.disconnect(ACCOUNT_DENIED_KICK);
            }

            stateStore.clear(playerUuid);
        }
    }
}
