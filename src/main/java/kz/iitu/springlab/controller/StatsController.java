package kz.iitu.springlab.controller;

import kz.iitu.springlab.aspect.CallCounterAspect;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatsController {

    private final CallCounterAspect callCounterAspect;

    public StatsController(CallCounterAspect callCounterAspect) {
        this.callCounterAspect = callCounterAspect;
    }

    @GetMapping("/api/lab4/stats")
    public Map<String, Long> stats() {
        return callCounterAspect.getStatistics();
    }
}