package com.voyanta.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test helper for asserting that plan generation is genuinely ASYNCHRONOUS.
 *
 * It lets a test hold the AI call open on demand. If POST /api/plans/generate
 * still blocked on the request thread (the pre-fix behaviour), the call to
 * generatePlan(...) would not return until release() was invoked, and these
 * tests would deadlock/timeout instead of passing.
 *
 * Controlled through static state because the generator is a Spring singleton and
 * tests share one application context. Every entry point resets the latches, so
 * tests cannot leak state into each other.
 */
public final class SlowAiPlanGenerator {

    private static final AtomicReference<CountDownLatch> HELD = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch> STARTED = new AtomicReference<>();
    private static final AtomicReference<Thread> AI_THREAD = new AtomicReference<>();
    private static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();

    private SlowAiPlanGenerator() {
    }

    /** Makes the next AI call block until {@link #release()} is called. */
    public static void holdNextCall() {
        HELD.set(new CountDownLatch(1));
        STARTED.set(new CountDownLatch(1));
        AI_THREAD.set(null);
        FAILURE.set(null);
    }

    public static void release() {
        CountDownLatch held = HELD.get();
        if (held != null) {
            held.countDown();
        }
    }

    /** Resets state and unblocks anything still waiting, so a failed test cannot hang the suite. */
    public static void reset() {
        release();
        HELD.set(null);
        STARTED.set(null);
        AI_THREAD.set(null);
        FAILURE.set(null);
    }

    public static void failWith(Throwable failure) {
        FAILURE.set(failure);
    }

    /**
     * Blocks while a hold is active; records which thread the AI call actually ran on.
     *
     * The hold is a no-op unless {@link #holdNextCall()} was called, so tests that do
     * not care about timing are completely unaffected by this helper.
     */
    public static void beforeGenerate() {
        Throwable failure = FAILURE.get();
        if (failure != null) {
            throw new RuntimeException(failure);
        }

        CountDownLatch held = HELD.get();
        if (held == null) {
            return;
        }

        AI_THREAD.set(Thread.currentThread());

        CountDownLatch started = STARTED.get();
        if (started != null) {
            // Signals that the worker really reached the AI call, so the test can assert
            // on that instead of guessing with a sleep.
            started.countDown();
        }

        try {
            // Bounded so a forgotten release() cannot park a pool thread forever.
            if (!held.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("held AI call was never released by the test");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while holding the AI call", e);
        }
    }

    /** Blocks until the AI call has actually started (or the timeout expires). */
    public static boolean awaitAiCallStarted(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch started = STARTED.get();
        return started != null && started.await(timeout, unit);
    }

    /** The thread on which the AI provider call executed; null until it has started. */
    public static Thread aiThread() {
        return AI_THREAD.get();
    }
}
