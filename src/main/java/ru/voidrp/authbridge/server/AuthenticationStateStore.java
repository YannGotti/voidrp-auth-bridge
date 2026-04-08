package ru.voidrp.authbridge.server;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthenticationStateStore {

    private final Map<UUID, AuthenticatedPlayerRecord> records = new ConcurrentHashMap<>();
    private final Map<UUID, PendingPlayerRecord> pendingRecords = new ConcurrentHashMap<>();
    private final Map<UUID, PendingPlayerRecord> legacyPendingRecords = new ConcurrentHashMap<>();

    public void markAuthenticated(AuthenticatedPlayerRecord record) {
        records.put(record.playerUuid(), record);
        pendingRecords.remove(record.playerUuid());
        legacyPendingRecords.remove(record.playerUuid());
    }

    public void markPending(UUID playerUuid, PendingPlayerRecord pending) {
        pendingRecords.put(playerUuid, pending);
        legacyPendingRecords.remove(playerUuid);
    }

    public Optional<PendingPlayerRecord> findPending(UUID playerUuid) {
        return Optional.ofNullable(pendingRecords.get(playerUuid));
    }

    public Optional<PendingPlayerRecord> findLegacyPending(UUID playerUuid) {
        return Optional.ofNullable(legacyPendingRecords.get(playerUuid));
    }

    public Optional<PendingPlayerRecord> findRestriction(UUID playerUuid) {
        PendingPlayerRecord pending = pendingRecords.get(playerUuid);
        if (pending != null) {
            return Optional.of(pending);
        }

        return Optional.ofNullable(legacyPendingRecords.get(playerUuid));
    }

    public void markLegacyPending(UUID playerUuid) {
        PendingPlayerRecord pending = pendingRecords.remove(playerUuid);
        if (pending != null) {
            legacyPendingRecords.put(playerUuid, pending);
        }
    }

    public boolean isLegacyPending(UUID playerUuid) {
        return legacyPendingRecords.containsKey(playerUuid);
    }

    public void clear(UUID playerUuid) {
        records.remove(playerUuid);
        pendingRecords.remove(playerUuid);
        legacyPendingRecords.remove(playerUuid);
    }

    public boolean isAuthenticated(UUID playerUuid) {
        return records.containsKey(playerUuid);
    }

    public boolean isRestricted(UUID playerUuid) {
        return !records.containsKey(playerUuid)
                && (pendingRecords.containsKey(playerUuid) || legacyPendingRecords.containsKey(playerUuid));
    }

    public Optional<AuthenticatedPlayerRecord> find(UUID playerUuid) {
        return Optional.ofNullable(records.get(playerUuid));
    }

    public Map<UUID, PendingPlayerRecord> snapshotPending() {
        return Map.copyOf(pendingRecords);
    }

    public record PendingPlayerRecord(
            Instant deadlineUtc,
            boolean legacyAuthEnabled,
            boolean mustUseLauncher,
            double anchorX,
            double anchorY,
            double anchorZ
    ) {
    }
}
