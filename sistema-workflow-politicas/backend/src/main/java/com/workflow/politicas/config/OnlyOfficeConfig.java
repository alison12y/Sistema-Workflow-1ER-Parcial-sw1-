package com.workflow.politicas.config;

import com.workflow.politicas.storage.OnlyOfficeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OnlyOfficeProperties.class)
public class OnlyOfficeConfig {
}
