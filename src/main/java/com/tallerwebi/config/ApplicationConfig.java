package com.tallerwebi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(GoogleCalendarConfig.class)
@PropertySource(value = "file:application.properties", ignoreResourceNotFound = true)
public class ApplicationConfig {}
