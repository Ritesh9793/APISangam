package com.apimarketplace.controller;

import com.apimarketplace.dto.api.ApiProductResponse;
import com.apimarketplace.service.ApiSearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ApiSearchService apiSearchService;

    public SearchController(ApiSearchService apiSearchService) {
        this.apiSearchService = apiSearchService;
    }

    @GetMapping("/apis")
    public List<ApiProductResponse> search(
        @RequestParam(required = false, name = "q") String query,
        @RequestParam(required = false) String category
    ) {
        return apiSearchService.search(query, category);
    }
}
