package com.loadup.assessment.order.config;

import com.zaxxer.hikari.HikariDataSource;
import com.loadup.assessment.order.config.TenantDatabaseProperties.TenantDataSourceProperties;
import com.loadup.assessment.order.tenant.TenantContext;
import org.flywaydb.core.Flyway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(TenantDatabaseProperties.class)
public class DataSourceConfig {
    private final String migrationLocation;

    public DataSourceConfig(@Value("${app.flyway-location:classpath:db/order-migration}") final String migrationLocation) {
        this.migrationLocation = migrationLocation;
    }

    @Bean
    @Primary
    public DataSource dataSource(final TenantDatabaseProperties tenantDatabaseProperties) {
        Map<Object, Object> tenantDataSources = new LinkedHashMap<>();
        Set<String> tenantIds = new LinkedHashSet<>();
        String schema = tenantDatabaseProperties.getSchema();
        tenantDatabaseProperties.getTenants().forEach((tenantId, properties) -> {
            DataSource tenantDataSource = buildDataSource(properties, schema);
            migrate(tenantDataSource, schema);
            tenantDataSources.put(tenantId, tenantDataSource);
            tenantIds.add(tenantId);
        });
        if (tenantDataSources.isEmpty()) {
            throw new IllegalStateException("No tenant databases configured");
        }
        String defaultTenantId = tenantIds.iterator().next();
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(defaultTenantId, tenantIds);
        routingDataSource.setTargetDataSources(tenantDataSources);
        routingDataSource.setDefaultTargetDataSource(tenantDataSources.get(defaultTenantId));
        routingDataSource.afterPropertiesSet();
        TenantContext.setTenantId(defaultTenantId);
        return routingDataSource;
    }

    private DataSource buildDataSource(final TenantDataSourceProperties properties, final String schema) {
        DataSourceBuilder<HikariDataSource> builder = DataSourceBuilder.create().type(HikariDataSource.class);
        builder.url(withCurrentSchema(properties.getUrl(), schema));
        builder.username(properties.getUsername());
        builder.password(properties.getPassword());
        if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
            builder.driverClassName(properties.getDriverClassName());
        }
        return builder.build();
    }

    private void migrate(final DataSource dataSource, final String schema) {
        Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema(schema)
                .schemas(schema)
                .locations(migrationLocation)
                .load()
                .migrate();
    }

    private String withCurrentSchema(final String url, final String schema) {
        if (schema == null || schema.isBlank() || url == null || !url.startsWith("jdbc:postgresql:")) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "currentSchema=" + schema;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void clearBootstrapTenantContext() {
        TenantContext.clear();
    }
}
