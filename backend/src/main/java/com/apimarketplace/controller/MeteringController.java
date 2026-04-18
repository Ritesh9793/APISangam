package com.apimarketplace.controller;

import com.apimarketplace.dto.metering.ApiCallLogRequest;
import com.apimarketplace.dto.metering.ApiCallLogResponse;
import com.apimarketplace.service.MeteringService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metering")
public class MeteringController {

    private final MeteringService meteringService;

    public MeteringController(MeteringService meteringService) {
        this.meteringService = meteringService;
    }

    @PostMapping("/calls")
    public ApiCallLogResponse log(@Valid @RequestBody ApiCallLogRequest request) {
        return meteringService.record(request);
    }
}
