package ru.voidrp.authbridge.server;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;

public final class ServerAuthHooks {
    private ServerAuthHooks() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ModBootstrap.get().stateStore().clear(event.getEntity().getUUID());
    }
}