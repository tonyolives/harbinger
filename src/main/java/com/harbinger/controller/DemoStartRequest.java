package com.harbinger.controller;

/**
 * Parameters for {@code POST /api/v1/demo/start}, posted by the UI control panel. Any field left
 * null falls back to the demo defaults (the same constants the project shipped with), so a bare
 * {@code {}} starts the canonical clean run. Values are clamped to safe ranges in {@link #seed()}
 * etc. via {@link #normalized()} so a stray input can't spin up a runaway feed.
 */
public record DemoStartRequest(
        Long seed, Integer owners, Integer signalsPerOwner, Boolean hardMode, Long feedDelayMs) {

    private static final long DEFAULT_SEED = 2L;
    private static final int DEFAULT_OWNERS = 8;
    private static final int DEFAULT_SIGNALS_PER_OWNER = 6;
    private static final boolean DEFAULT_HARD_MODE = false;
    private static final long DEFAULT_FEED_DELAY_MS = 300L;

    private static final int MAX_OWNERS = 50;
    private static final int MAX_SIGNALS_PER_OWNER = 20;
    private static final long MAX_FEED_DELAY_MS = 2000L;

    /** Fill nulls with defaults and clamp every field to a safe range for a live demo. */
    public DemoStartRequest normalized() {
        return new DemoStartRequest(
                seed == null ? DEFAULT_SEED : seed,
                clamp(owners == null ? DEFAULT_OWNERS : owners, 1, MAX_OWNERS),
                clamp(signalsPerOwner == null ? DEFAULT_SIGNALS_PER_OWNER : signalsPerOwner,
                        1, MAX_SIGNALS_PER_OWNER),
                hardMode == null ? DEFAULT_HARD_MODE : hardMode,
                clampLong(feedDelayMs == null ? DEFAULT_FEED_DELAY_MS : feedDelayMs,
                        0L, MAX_FEED_DELAY_MS));
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private static long clampLong(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }
}
