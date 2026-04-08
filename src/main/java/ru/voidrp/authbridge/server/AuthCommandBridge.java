package ru.voidrp.authbridge.server;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.voidrp.authbridge.bootstrap.ModBootstrap;
import ru.voidrp.authbridge.common.dto.LegacyLoginResponse;

public final class AuthCommandBridge {
    private AuthCommandBridge() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("login")
                        .then(Commands.argument("password", StringArgumentType.greedyString())
                                .executes(context -> {
                                    var source = context.getSource();
                                    var player = source.getPlayerOrException();
                                    String password = StringArgumentType.getString(context, "password");

                                    if (!ModBootstrap.get().stateStore().isLegacyPending(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal("Для этого аккаунта legacy вход не разрешён."));
                                        return 0;
                                    }

                                    LegacyLoginResponse response = ModBootstrap.get().legacyAuthService().login(
                                            player.getUUID(),
                                            player.getGameProfile().getName(),
                                            password
                                    );

                                    if (response != null && response.accepted()) {
                                        ModBootstrap.get().authRestrictionBridge().onPlayerAuthenticated(player.getUUID());
                                        player.sendSystemMessage(Component.literal("Legacy авторизация успешна."));
                                        return 1;
                                    }

                                    String reason = response != null && response.error() != null
                                            ? response.error()
                                            : "invalid credentials";
                                    player.sendSystemMessage(Component.literal("Ошибка legacy входа: " + reason));
                                    return 0;
                                }))
        );
    }
}