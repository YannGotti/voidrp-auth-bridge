package ru.voidrp.authbridge.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.voidrp.authbridge.VoidRpAuthBridge;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketRequest;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketResponse;

public final class ServerPayloadHandler {

    private ServerPayloadHandler() {
    }

    public static void handleConsumePlayTicket(final ConsumePlayTicketPayload payload, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            VoidRpAuthBridge.LOGGER.warn("Received consume-play-ticket payload from a non-server-player context.");
            return;
        }

        VoidRpAuthBridge.LOGGER.info(
                "Received launcher ticket payload: player={} uuid={} requestedPlayerName={}",
                serverPlayer.getGameProfile().getName(),
                serverPlayer.getUUID(),
                payload.playerName()
        );

        // Always use the server-verified game profile name, never trust the client-supplied name.
        String verifiedPlayerName = serverPlayer.getGameProfile().getName();
        String ticket = payload.ticket() != null ? payload.ticket().strip() : "";
        String launcherProof = payload.launcherProof() != null ? payload.launcherProof().strip() : "";
        if (ticket.length() > 512) {
            ticket = ticket.substring(0, 512);
        }
        if (launcherProof.length() > 128) {
            launcherProof = launcherProof.substring(0, 128);
        }
        ConsumePlayTicketResponse response = ModBootstrap.get().playTicketConsumeService().authenticate(
                serverPlayer.getUUID(),
                verifiedPlayerName,
                new ConsumePlayTicketRequest(
                        ticket,
                        verifiedPlayerName,
                        launcherProof
                )
        );

        if (response != null && response.accepted()) {
            ModBootstrap.get().authRestrictionBridge().onPlayerAuthenticated(serverPlayer.getUUID());
            serverPlayer.sendSystemMessage(Component.literal("Авторизация через лаунчер подтверждена."));
            PacketDistributor.sendToPlayer(serverPlayer, AuthStatusPayload.accepted("Авторизация подтверждена"));

            VoidRpAuthBridge.LOGGER.info(
                    "Launcher auth accepted: player={} uuid={} userId={}",
                    serverPlayer.getGameProfile().getName(),
                    serverPlayer.getUUID(),
                    response.userId()
            );
        } else {
            String reason = response != null && response.error() != null
                    ? response.error()
                    : "launcher ticket rejected";

            serverPlayer.sendSystemMessage(Component.literal("Ticket авторизация не прошла: " + reason));
            PacketDistributor.sendToPlayer(serverPlayer, AuthStatusPayload.rejected(reason));

            VoidRpAuthBridge.LOGGER.warn(
                    "Launcher auth rejected: player={} uuid={} reason={}",
                    serverPlayer.getGameProfile().getName(),
                    serverPlayer.getUUID(),
                    reason
            );
        }
    }
}
