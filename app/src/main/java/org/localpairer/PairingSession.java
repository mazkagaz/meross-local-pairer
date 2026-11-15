package org.localpairer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Ensures only the most recently started pairing screen can act. A pairing
 * screen left in the back stack must not keep configuring devices or verify
 * another attempt after the user started a new one.
 */
public final class PairingSession {
    private static final AtomicLong CURRENT = new AtomicLong(0);

    private PairingSession() {
    }

    public static long begin() {
        return CURRENT.incrementAndGet();
    }

    public static boolean isCurrent(long sessionId) {
        return sessionId != 0 && CURRENT.get() == sessionId;
    }

    public static void invalidate(long sessionId) {
        CURRENT.compareAndSet(sessionId, sessionId + 1);
    }
}
