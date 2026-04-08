package ru.voidrp.authbridge.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketRequest;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketResponse;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    public static void handleConsumePlayTicket(final ConsumePlayTicketPayload payload, final IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ConsumePlayTicketResponse response = ModBootstrap.get().playTicketConsumeService().authenticate(
                serverPlayer.getUUID(),
                serverPlayer.getGameProfile().getName(),
                new ConsumePlayTicketRequest(
                        payload.ticket(),
                        payload.playerName()
                )
        );

        if (response != null && response.accepted()) {
            ModBootstrap.get().authRestrictionBridge().onPlayerAuthenticated(serverPlayer.getUUID());
            serverPlayer.sendSystemMessage(Component.literal("Авторизация через лаунчер подтверждена."));
        } else {
            String reason = response != null && response.error() != null
                    ? response.error()
                    : "launcher ticket rejected";
            serverPlayer.sendSystemMessage(Component.literal("Ticket авторизация не прошла: " + reason));
        }
    }
}