package de.fraunhofer.iais.eis.oihplus.controller;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ForkJoinPool;

import de.fraunhofer.iais.eis.oihplus.config.ConfigLogger;
import de.fraunhofer.iais.eis.oihplus.service.DSCService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@Slf4j
@RestController
@EnableConfigurationProperties(ConfigLogger.class)
public class DSCController {

    @Autowired
    private DSCService dscService;

    @Autowired
    private ConfigLogger logger;

    // a map to handle the payload coming from the hook
    private Map<String, Object> payload;

    // a flag for payload update (true: payload updated, then finalize the GET request!)
    private boolean payloadUpdated;

    @GetMapping( "/service" )
    public DeferredResult<ResponseEntity<?>> startGETRequest(
            @RequestParam String flowId,
            @RequestParam List<String> filter ) {
        payloadUpdated = false;

        if (logger.enabled) {
            log.info("New Request received at /service endpoint");}


        dscService.callHook(flowId,  filter);
        // DeferredResult: the application can produce the result from a thread of its choice.
        DeferredResult<ResponseEntity<?>> output = new DeferredResult<>();

        ForkJoinPool.commonPool().submit(() -> {
            var timer = new Timer();
            if (logger.enabled) {
                log.info("Waiting for POST from Flow");}
            timer.schedule(new TimerTask() {
                public void run() {
                    if( payloadUpdated ) {
                        payloadUpdated = false;
                        if (logger.enabled) {
                            log.info("Result received");}
                        output.setResult(ResponseEntity.ok(getPayload()
                        ));
                        timer.cancel();
                    }
                }
            }, 0, 500);

        });


        return output;
    }

    /*
        A controller that listens to webhook and parse the payload
    */

    @PostMapping( value = "/webhook", consumes = "application/json" )
    public void webhook( @RequestBody Map<String, Object> payload ) {
        if (logger.enabled) {
            log.info("New Request received at /webhook endpoint");}

        setPayload(payload);

        if (logger.enabled) {
            log.info(String.format("payload set to %s", payload));}
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload( Map<String, Object> payload ) {
        this.payload = payload;
        payloadUpdated = true;
    }

    @PostMapping( value = "/test", consumes = "application/json" )
    public ResponseEntity<String> test(@RequestBody String body) {
        if (logger.enabled) {
            log.info(String.format(
                    "New Request received at /test endpoint with body: %s",body ));}
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
