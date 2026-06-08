package com.harbinger.service.pipeline;

import com.harbinger.model.Lead;
import com.harbinger.model.RawSignal;
import com.harbinger.service.SignalGeneratorService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Drives the live demo from the UI: generates a fresh batch of synthetic signals and replays them
 * one at a time through {@link RealtimeLeadService} on a paced daemon thread, so leads trickle in
 * over the SSE stream. Each call to {@link #start} first stops any running feed and resets all
 * live state, so pressing "Begin" mid-run cleanly restarts from empty.
 *
 * <p>Pure presentation glue — all pipeline logic lives in (and is tested via) the services. This
 * class only owns the feeder thread, so it is excluded from the coverage gate like {@code
 * DemoRunner}.
 */
@Service
public class DemoFeedService {

    private final SignalGeneratorService generator;
    private final RealtimeLeadService realtimeLeadService;

    private volatile Thread feeder;

    public DemoFeedService(
            SignalGeneratorService generator, RealtimeLeadService realtimeLeadService) {
        this.generator = generator;
        this.realtimeLeadService = realtimeLeadService;
    }

    /**
     * Stop any running feed, clear all leads, generate a fresh batch, and start replaying it.
     *
     * @param feedDelayMs delay between signals so leads visibly trickle in over SSE
     */
    public synchronized void start(
            long seed, int owners, int signalsPerOwner, boolean hardMode, long feedDelayMs) {
        stop();
        realtimeLeadService.reset();

        List<RawSignal> signals = generator.generate(seed, owners, signalsPerOwner, hardMode);
        Thread thread = new Thread(() -> replay(signals, feedDelayMs), "demo-feed");
        thread.setDaemon(true);
        feeder = thread;
        thread.start();
    }

    /** Stop the running feed (if any) and clear all live state back to empty. */
    public synchronized void reset() {
        stop();
        realtimeLeadService.reset();
    }

    /** Interrupt and forget the current feeder thread; no-op when nothing is running. */
    public synchronized void stop() {
        Thread current = feeder;
        if (current != null) {
            current.interrupt();
            feeder = null;
        }
    }

    /** Ingest each signal {@code feedDelayMs} apart; stops early if interrupted by a new run. */
    private void replay(List<RawSignal> signals, long feedDelayMs) {
        for (RawSignal signal : signals) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            for (Lead ignored : realtimeLeadService.ingest(signal)) {
                // Surfacings publish to SSE inside RealtimeLeadService; nothing to do here.
            }
            try {
                Thread.sleep(feedDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
