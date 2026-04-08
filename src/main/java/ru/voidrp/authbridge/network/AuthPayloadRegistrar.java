package ru.voidrp.authbridge.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AuthPayloadRegistrar {
    private AuthPayloadRegistrar() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ConsumePlayTicketPayload.TYPE,
                ConsumePlayTicketPayload.STREAM_CODEC,
                ServerPayloadHandler::handleConsumePlayTicket
        );
    }
}