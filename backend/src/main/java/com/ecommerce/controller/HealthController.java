package com.ecommerce.controller;

import com.ecommerce.util.Result;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final ApplicationContext applicationContext;
    private final Environment environment;
    private final BuildProperties buildProperties;

    public HealthController(ApplicationContext applicationContext,
                            Environment environment,
                            org.springframework.beans.factory.ObjectProvider<BuildProperties> buildProperties) {
        this.applicationContext = applicationContext;
        this.environment = environment;
        this.buildProperties = buildProperties.getIfAvailable();
    }

    @GetMapping("/health")
    public Result<?> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("application", applicationContext.getApplicationName());
        data.put("profiles", environment.getActiveProfiles());
        data.put("version", buildProperties == null ? "dev" : buildProperties.getVersion());
        data.put("timestamp", Instant.now().toString());
        return Result.success(data);
    }
}
