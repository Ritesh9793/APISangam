package com.apimarketplace.controller;

import com.apimarketplace.dto.api.ApiProductRequest;
import com.apimarketplace.dto.api.ApiProductResponse;
import com.apimarketplace.security.UserPrincipal;
import com.apimarketplace.service.ApiCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/apis")
public class ApiController {

    private final ApiCatalogService apiCatalogService;

    public ApiController(ApiCatalogService apiCatalogService) {
        this.apiCatalogService = apiCatalogService;
    }

    @GetMapping
    public List<ApiProductResponse> list(
        @RequestParam(required = false) String category,
        @RequestParam(required = false, name = "q") String query
    ) {
        return apiCatalogService.list(category, query);
    }

    @GetMapping("/{id}")
    public ApiProductResponse get(@PathVariable UUID id) {
        return apiCatalogService.get(id);
    }

    @PostMapping
    public ApiProductResponse create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ApiProductRequest request
    ) {
        return apiCatalogService.create(principal, request);
    }
}
