package buildcraft.builders.snapshot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player guard for client snapshot fetches. Requests are optional client cache fills, so dropping an
 * abusive burst is preferable to letting arbitrary cache misses consume server filesystem/CPU time.
 */
final class SnapshotRequestLimiter {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final long STALE_NANOS = 60_000_000_000L;
    private static final int MAX_REQUESTS_PER_WINDOW = 32;
    private static final int CLEANUP_INTERVAL = 256;

    private static final Map<UUID, Window> WINDOWS = new HashMap<>();
    private static int callsUntilCleanup = CLEANUP_INTERVAL;

    private SnapshotRequestLimiter() {}

    static synchronized boolean allow(ServerPlayer player) {
        long now = System.nanoTime();
        UUID id = player.getUUID();
        Window window = WINDOWS.get(id);
        if (window == null || now - window.startedAt >= WINDOW_NANOS) {
            WINDOWS.put(id, new Window(now, 1));
            cleanupIfNeeded(now);
            return true;
        }
        window.lastSeen = now;
        if (window.count >= MAX_REQUESTS_PER_WINDOW) {
            cleanupIfNeeded(now);
            return false;
        }
        window.count++;
        cleanupIfNeeded(now);
        return true;
    }

    private static void cleanupIfNeeded(long now) {
        if (--callsUntilCleanup > 0 && WINDOWS.size() <= 1024) {
            return;
        }
        callsUntilCleanup = CLEANUP_INTERVAL;
        Iterator<Map.Entry<UUID, Window>> iterator = WINDOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastSeen >= STALE_NANOS) {
                iterator.remove();
            }
        }
    }

    private static final class Window {
        private final long startedAt;
        private int count;
        private long lastSeen;

        private Window(long now, int count) {
            this.startedAt = now;
            this.count = count;
            this.lastSeen = now;
        }
    }
}
