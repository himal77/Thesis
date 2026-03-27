package com.IoT.query.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryApiConfig {

    // Max rows returned per query — prevents Grafana from
    // pulling millions of rows and killing PostgreSQL
    @Value("${query.max-results:1000}")
    private int maxResults;

    // Default lookback window in minutes for "recent" queries
    @Value("${query.default-lookback-minutes:60}")
    private int defaultLookbackMinutes;

    public int getMaxResults()             { return maxResults; }
    public int getDefaultLookbackMinutes() { return defaultLookbackMinutes; }
}