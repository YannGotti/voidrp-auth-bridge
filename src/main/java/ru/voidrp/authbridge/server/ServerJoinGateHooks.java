package ru.voidrp.authbridge.server;

import java.time.Instant;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;
import ru.voidrp.authbridge.common.dto.PlayerAccessResponse;

public final class ServerJoinGateHooks {

    private static final long AUTH_GRACE_SECONDS = 5L;

    private static final Component WAITING_FOR_LAUNCHER = Component.literal(
            "Ожидаем подтверждение входа через лаунчер VoidRP..."
    );
    private static final Component WAITING_FOR_TICKET_OR_LOGIN = Component.literal(
            "Проверяем вход. Если launcher ticket не придёт, используй /login <password>."
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

    private ServerJoinGateHooks() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerUuid = player.getUUID();
        String playerName = player.getGameProfile().getName();

        PlayerAccessResponse access = ModBootstrap.get().backendAuthClient().getPlayerAccess(playerName);

        ModBootstrap.get().stateStore().markPending(
                playerUuid,
                new AuthenticationStateStore.PendingPlayerRecord(
                        Instant.now().plusSeconds(AUTH_GRACE_SECONDS),
                        access.legacyAuthEnabled(),
                        access.mustUseLauncher(),
                        player.getX(),
                        player.getY(),
                        player.getZ()
                )
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
                continue;
            }

            if (record.mustUseLauncher()) {
                player.connection.disconnect(LAUNCHER_ONLY_KICK);
            } else {
                player.connection.disconnect(ACCOUNT_DENIED_KICK);
            }

            stateStore.clear(playerUuid);
        }
    }
}
