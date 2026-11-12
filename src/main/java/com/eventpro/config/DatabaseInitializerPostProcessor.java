package com.eventpro.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

public class DatabaseInitializerPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        if (!environment.getProperty("app.database.auto-create", Boolean.class, false)) return;

        String urlPrefix = environment.getRequiredProperty("app.database.url-prefix");
        String dbName = environment.getRequiredProperty("app.database.name");
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");

        DatabaseInitializer.ensureDatabaseExists(urlPrefix, dbName, username, password);
    }
}
