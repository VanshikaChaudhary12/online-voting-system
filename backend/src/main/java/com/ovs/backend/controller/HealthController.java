package com.ovs.backend.controller;

import com.ovs.backend.dto.ApiMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiMessage health() {
        return new ApiMessage("Online Voting System API is running");
    }
}
