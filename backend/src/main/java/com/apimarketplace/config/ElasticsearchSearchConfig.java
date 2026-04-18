package com.apimarketplace.config;

import com.apimarketplace.search.ApiProductSearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(prefix = "app.search", name = "elasticsearch-enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackageClasses = ApiProductSearchRepository.class)
public class ElasticsearchSearchConfig {
}
