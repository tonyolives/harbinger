package com.harbinger.controller;

import com.harbinger.service.pipeline.DemoFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the UI drive the live demo: start a fresh paced feed with chosen signal parameters, or
 * reset back to empty. Thin glue over {@link DemoFeedService}; the surfacing itself flows out over
 * the existing {@code /stream} SSE endpoint. Excluded from the coverage gate as presentation glue.
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoFeedService demoFeedService;

    public DemoController(DemoFeedService demoFeedService) {
        this.demoFeedService = demoFeedService;
    }

    /** Begin (or restart) the live feed with the given parameters; defaults fill any nulls. */
    @PostMapping("/start")
    public ResponseEntity<Void> start(@RequestBody(required = false) DemoStartRequest request) {
        DemoStartRequest params =
                (request == null ? new DemoStartRequest(null, null, null, null, null) : request)
                        .normalized();
        demoFeedService.start(
                params.seed(),
                params.owners(),
                params.signalsPerOwner(),
                params.hardMode(),
                params.feedDelayMs());
        return ResponseEntity.accepted().build();
    }

    /** Stop the feed and clear all surfaced leads. */
    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        demoFeedService.reset();
        return ResponseEntity.ok().build();
    }
}
