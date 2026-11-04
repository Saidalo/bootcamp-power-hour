package com.bootcamp.powerhour.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class EleringClientConfiguration {
    @Bean
    public RestClient eleringRestClient(@Value("${elering.base-url") String baseUrl, @Value("${elering.connect-timeout-ms") long connectTimeoutMs, @Value("${elering.read-timeout-ms") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
