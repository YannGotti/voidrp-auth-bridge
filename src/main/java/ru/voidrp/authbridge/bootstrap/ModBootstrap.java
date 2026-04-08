package ru.voidrp.authbridge.bootstrap;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import ru.voidrp.authbridge.VoidRpAuthBridge;
import ru.voidrp.authbridge.client.ClientAuthHooks;
import ru.voidrp.authbridge.client.ClientTicketDispatcher;
import ru.voidrp.authbridge.config.AuthBridgeProperties;
import ru.voidrp.authbridge.integration.AuthRestrictionBridge;
import ru.voidrp.authbridge.network.AuthPayloadRegistrar;
import ru.voidrp.authbridge.server.AuthCommandBridge;
import ru.voidrp.authbridge.server.AuthenticationStateStore;
import ru.voidrp.authbridge.server.BackendAuthClient;
import ru.voidrp.authbridge.server.LegacyAuthService;
import ru.voidrp.authbridge.server.PlayTicketConsumeService;
import ru.voidrp.authbridge.server.PreAuthRestrictionService;
import ru.voidrp.authbridge.server.ServerAuthHooks;
import ru.voidrp.authbridge.server.ServerJoinGateHooks;

public final class ModBootstrap {

    private static ModBootstrap INSTANCE;

    private final AuthBridgeProperties properties;
    private final AuthenticationStateStore stateStore;
    private final BackendAuthClient backendAuthClient;
    private final PlayTicketConsumeService playTicketConsumeService;
    private final LegacyAuthService legacyAuthService;
    private final ClientTicketDispatcher clientTicketDispatcher;
    private final AuthRestrictionBridge authRestrictionBridge;

    private ModBootstrap(
            AuthBridgeProperties properties,
            AuthenticationStateStore stateStore,
            BackendAuthClient backendAuthClient,
            PlayTicketConsumeService playTicketConsumeService,
            LegacyAuthService legacyAuthService,
            ClientTicketDispatcher clientTicketDispatcher,
            AuthRestrictionBridge authRestrictionBridge
    ) {
        this.properties = properties;
        this.stateStore = stateStore;
        this.backendAuthClient = backendAuthClient;
        this.playTicketConsumeService = playTicketConsumeService;
        this.legacyAuthService = legacyAuthService;
        this.clientTicketDispatcher = clientTicketDispatcher;
        this.authRestrictionBridge = authRestrictionBridge;
    }

    public static ModBootstrap createDefault() {
        AuthBridgeProperties properties = AuthBridgeProperties.loadDefault();
        AuthenticationStateStore stateStore = new AuthenticationStateStore();
        BackendAuthClient backendAuthClient = new BackendAuthClient(properties);

        INSTANCE = new ModBootstrap(
                properties,
                stateStore,
                backendAuthClient,
                new PlayTicketConsumeService(backendAuthClient, stateStore),
                new LegacyAuthService(backendAuthClient, stateStore),
                new ClientTicketDispatcher(properties),
                AuthRestrictionBridge.noop()
        );

        return INSTANCE;
    }

    public static ModBootstrap get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ModBootstrap is not initialized yet");
        }
        return INSTANCE;
    }

    public void initialize(IEventBus modBus) {
        if (modBus == null) {
            throw new IllegalArgumentException("modBus must not be null");
        }

        VoidRpAuthBridge.LOGGER.info("Bootstrapping auth bridge against backend {}", properties.backendBaseUrl());

        modBus.register(AuthPayloadRegistrar.class);

        NeoForge.EVENT_BUS.register(AuthCommandBridge.class);
        NeoForge.EVENT_BUS.register(ServerAuthHooks.class);
        NeoForge.EVENT_BUS.register(ServerJoinGateHooks.class);
        NeoForge.EVENT_BUS.register(PreAuthRestrictionService.class);
        NeoForge.EVENT_BUS.register(ClientAuthHooks.class);

        VoidRpAuthBridge.LOGGER.info(
                "Auth bridge registered: stateStore={}, backendClient={}, playTicketService={}, legacyService={}, preAuthRestrictionService={}",
                stateStore.getClass().getSimpleName(),
                backendAuthClient.getClass().getSimpleName(),
                playTicketConsumeService.getClass().getSimpleName(),
                legacyAuthService.getClass().getSimpleName(),
                PreAuthRestrictionService.class.getSimpleName()
        );
    }

    public AuthBridgeProperties properties() {
        return properties;
    }

    public AuthenticationStateStore stateStore() {
        return stateStore;
    }

    public BackendAuthClient backendAuthClient() {
        return backendAuthClient;
    }

    public PlayTicketConsumeService playTicketConsumeService() {
        return playTicketConsumeService;
    }

    public LegacyAuthService legacyAuthService() {
        return legacyAuthService;
    }

    public ClientTicketDispatcher clientTicketDispatcher() {
        return clientTicketDispatcher;
    }

    public AuthRestrictionBridge authRestrictionBridge() {
        return authRestrictionBridge;
    }
}
