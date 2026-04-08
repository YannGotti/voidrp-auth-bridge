package ru.voidrp.authbridge.client;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;
import ru.voidrp.authbridge.common.dto.ConsumePlayTicketRequest;
import ru.voidrp.authbridge.network.ConsumePlayTicketPayload;

public final class ClientAuthHooks {
    private static boolean sentThisSession = false;

    private ClientAuthHooks() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        sentThisSession = false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        String playerName = minecraft.player.getGameProfile().getName();

        Optional<ConsumePlayTicketRequest> request =
                ModBootstrap.get().clientTicketDispatcher().buildConsumeRequest(playerName);

        if (request.isEmpty()) {
            return;
        }

        if (sentThisSession) {
            return;
        }

        ConsumePlayTicketRequest value = request.get();
        PacketDistributor.sendToServer(new ConsumePlayTicketPayload(
                value.ticket(),
                value.playerName()
        ));
        sentThisSession = true;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        sentThisSession = false;
    }
}