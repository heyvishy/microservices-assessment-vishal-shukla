package com.loadup.assessment.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadup.assessment.notification.NotificationServiceApplication;
import com.loadup.assessment.order.OrderServiceApplication;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class OrderNotificationFlowIT {
    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("postgres")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.1"));

    private static ConfigurableWebServerApplicationContext orderContext;
    private static ConfigurableWebServerApplicationContext notificationContext;
    private static String orderServiceUrl;
    private static String notificationServiceUrl;

    @BeforeAll
    static void startServices() throws Exception {
        initializeTenantDatabases();
        notificationContext = startApplication(NotificationServiceApplication.class, notificationProperties());
        orderContext = startApplication(OrderServiceApplication.class, orderProperties());
        orderServiceUrl = serviceUrl(orderContext);
        notificationServiceUrl = serviceUrl(notificationContext);
    }

    @AfterAll
    static void stopServices() {
        if (orderContext != null) {
            orderContext.close();
        }
        if (notificationContext != null) {
            notificationContext.close();
        }
    }

    @Test
    void routesOrdersAndNotificationsAcrossTenantDatabases() throws Exception {
        JsonNode tenantAOrder = createOrder(TENANT_A, "e2e-a", "tenant-a@example.com");
        JsonNode tenantBOrder = createOrder(TENANT_B, "e2e-b", "tenant-b@example.com");

        String tenantAOrderId = tenantAOrder.get("id").asText();
        String tenantBOrderId = tenantBOrder.get("id").asText();
        assertEquals("CONFIRMED", tenantAOrder.get("status").asText());
        assertEquals("CONFIRMED", tenantBOrder.get("status").asText());

        assertEquals(200, getOrder(TENANT_A, tenantAOrderId).statusCode());
        assertEquals(200, getOrder(TENANT_B, tenantBOrderId).statusCode());
        assertEquals(404, getOrder(TENANT_B, tenantAOrderId).statusCode());
        assertEquals(404, getOrder(TENANT_A, tenantBOrderId).statusCode());

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    assertNotificationExists(TENANT_A, tenantAOrderId);
                    assertNotificationExists(TENANT_B, tenantBOrderId);
                    assertEquals(1, countRows("tenant_a_db", "notification_service.notifications"));
                    assertEquals(1, countRows("tenant_b_db", "notification_service.notifications"));
                    assertEquals(1, countPublishedOutboxRows("tenant_a_db"));
                    assertEquals(1, countPublishedOutboxRows("tenant_b_db"));
                });

        assertEquals(1, countRows("tenant_a_db", "order_service.orders"));
        assertEquals(1, countRows("tenant_b_db", "order_service.orders"));
        assertEquals(0, countOrder("tenant_a_db", tenantBOrderId));
        assertEquals(0, countOrder("tenant_b_db", tenantAOrderId));
    }

    private static ConfigurableWebServerApplicationContext startApplication(
            final Class<?> applicationClass,
            final Map<String, Object> properties) {
        return (ConfigurableWebServerApplicationContext) new SpringApplicationBuilder(applicationClass)
                .web(WebApplicationType.SERVLET)
                .registerShutdownHook(false)
                .properties(properties)
                .run();
    }

    private static Map<String, Object> commonProperties(final String schema, final String migrationLocation) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.port", "0");
        properties.put("spring.config.name", "e2e");
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.jmx.enabled", "false");
        properties.put("spring.jpa.open-in-view", "false");
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("spring.jpa.properties.hibernate.default_schema", schema);
        properties.put("spring.flyway.enabled", "false");
        properties.put("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());
        properties.put("spring.kafka.consumer.auto-offset-reset", "earliest");
        properties.put("spring.kafka.consumer.key-deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("spring.kafka.consumer.value-deserializer",
                "org.springframework.kafka.support.serializer.JsonDeserializer");
        properties.put("spring.kafka.producer.key-serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("spring.kafka.producer.value-serializer",
                "org.springframework.kafka.support.serializer.JsonSerializer");
        properties.put("app.schema", schema);
        properties.put("app.flyway-location", migrationLocation);
        properties.put("logging.level.root", "WARN");
        return properties;
    }

    private static Map<String, Object> orderProperties() {
        Map<String, Object> properties = commonProperties("order_service", "classpath:db/order-migration");
        addTenantProperties(properties, "app.tenants", "order_app", "order_app");
        properties.put("app.kafka.order-topic", "orders.events.v1");
        properties.put("app.kafka.outbox-publish-delay-ms", "100");
        return properties;
    }

    private static Map<String, Object> notificationProperties() {
        Map<String, Object> properties = commonProperties(
                "notification_service", "classpath:db/notification-migration");
        addTenantProperties(properties, "app.tenants", "notification_app", "notification_app");
        properties.put("app.kafka.order-topic", "orders.events.v1");
        return properties;
    }

    private static void addTenantProperties(final Map<String, Object> properties,
                                            final String prefix,
                                            final String username,
                                            final String password) {
        properties.put(prefix + ".tenant-a.url", tenantJdbcUrl("tenant_a_db"));
        properties.put(prefix + ".tenant-a.username", username);
        properties.put(prefix + ".tenant-a.password", password);
        properties.put(prefix + ".tenant-b.url", tenantJdbcUrl("tenant_b_db"));
        properties.put(prefix + ".tenant-b.username", username);
        properties.put(prefix + ".tenant-b.password", password);
    }

    private static void initializeTenantDatabases() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute("create user order_app with password 'order_app'");
            statement.execute("create user notification_app with password 'notification_app'");
            statement.execute("create database tenant_a_db owner postgres");
            statement.execute("create database tenant_b_db owner postgres");
        }
        initializeTenantDatabase("tenant_a_db");
        initializeTenantDatabase("tenant_b_db");
    }

    private static void initializeTenantDatabase(final String database) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                tenantJdbcUrl(database), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("drop schema public cascade");
            statement.execute("create schema order_service authorization order_app");
            statement.execute("create schema notification_service authorization notification_app");
            statement.execute("grant usage, create on schema order_service to order_app");
            statement.execute("grant usage, create on schema notification_service to notification_app");
        }
    }

    private static JsonNode createOrder(final String tenantId,
                                        final String customerId,
                                        final String customerEmail) throws Exception {
        String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                "customerId", customerId,
                "customerEmail", customerEmail,
                "description", "Testcontainers end-to-end order",
                "totalAmount", new BigDecimal("49.95"),
                "currency", "USD"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(orderServiceUrl + "/api/v1/orders"))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), response.body());
        JsonNode responseBody = OBJECT_MAPPER.readTree(response.body());
        assertNotNull(responseBody.get("id"));
        return responseBody;
    }

    private static HttpResponse<String> getOrder(final String tenantId, final String orderId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(orderServiceUrl + "/api/v1/orders/" + orderId))
                .header("X-Tenant-Id", tenantId)
                .GET()
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void assertNotificationExists(final String tenantId, final String orderId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(notificationServiceUrl + "/api/v1/notifications"))
                .header("X-Tenant-Id", tenantId)
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        JsonNode notifications = OBJECT_MAPPER.readTree(response.body());
        assertTrue(notifications.isArray());
        boolean found = false;
        for (JsonNode notification : notifications) {
            if (orderId.equals(notification.get("orderId").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Notification not found for order " + orderId);
    }

    private static int countRows(final String database, final String table) throws Exception {
        return queryForInt(database, "select count(*) from " + table);
    }

    private static int countPublishedOutboxRows(final String database) throws Exception {
        return queryForInt(database,
                "select count(*) from order_service.order_outbox where published_at is not null");
    }

    private static int countOrder(final String database, final String orderId) throws Exception {
        UUID.fromString(orderId);
        return queryForInt(database,
                "select count(*) from order_service.orders where id = '" + orderId + "'::uuid");
    }

    private static int queryForInt(final String database, final String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                tenantJdbcUrl(database), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static String tenantJdbcUrl(final String database) {
        return POSTGRES.getJdbcUrl().replaceFirst("/postgres(?:\\?.*)?$", "/" + database);
    }

    private static String serviceUrl(final ConfigurableWebServerApplicationContext context) {
        return "http://localhost:" + context.getWebServer().getPort();
    }
}
