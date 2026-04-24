package com.eric6166.taskapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class AppProperties {
    private String secret;
    private long expirationMs;
}
