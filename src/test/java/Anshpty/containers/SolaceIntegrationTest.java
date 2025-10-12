package Anshpty.containers;

import Anshpty.containers.service.MessagePublisher;
import Anshpty.containers.service.MessageSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.solace.SolaceContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class SolaceIntegrationTest {

    @Container
    static SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:latest")
            .withVpn("default")
            .withCredentials("default", "default");



    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private MessageSubscriber messageSubscriber;

//    @DynamicPropertySource
//    static void solaceProperties(DynamicPropertyRegistry registry) {
//        registry.add("solace.java.host",
//                () -> solaceContainer.getOrigin(SolaceContainer.Service.SMF));
//        registry.add("solace.java.msg-vpn", () -> "default");
//        registry.add("solace.java.client-username", () -> "default");
//        registry.add("solace.java.client-password", () -> "default");
//        registry.add("app.solace.topic", () -> "demo/topic/test");
//    }

    @DynamicPropertySource
    static void solaceProperties(DynamicPropertyRegistry registry) {
        registry.add("solace.java.host",
                () -> "tcp://" + solaceContainer.getHost() + ":" + solaceContainer.getMappedPort(55555));
        registry.add("solace.java.msg-vpn", () -> "default");
        registry.add("solace.java.client-username", () -> "default");
        registry.add("solace.java.client-password", () -> "default");
        registry.add("app.solace.topic", () -> "demo/topic/test");
    }


    @BeforeEach
    void setUp() {
        messageSubscriber.clearReceivedMessages();
    }

    @Test
    void testSolaceContainerIsRunning() {
        assertTrue(solaceContainer.isRunning());
        assertNotNull(solaceContainer.getHost());
        assertTrue(solaceContainer.getMappedPort(55555) > 0);

    }

    @Test
    void testPublishAndSubscribeMessage() {
        // Given
        String testMessage = "Hello Solace from TestContainers!";

        // When
        messagePublisher.publishMessage(testMessage);

        // Then
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<String> receivedMessages = messageSubscriber.getReceivedMessages();
                    assertFalse(receivedMessages.isEmpty(), "Should have received at least one message");
                    assertTrue(receivedMessages.contains(testMessage),
                            "Received messages should contain the test message");
                });
    }

    @Test
    void testPublishMultipleMessages() {
        // Given
        String message1 = "First message";
        String message2 = "Second message";
        String message3 = "Third message";

        // When
        messagePublisher.publishMessage(message1);
        messagePublisher.publishMessage(message2);
        messagePublisher.publishMessage(message3);

        // Then
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<String> receivedMessages = messageSubscriber.getReceivedMessages();
                    assertEquals(3, receivedMessages.size(), "Should have received 3 messages");
                    assertTrue(receivedMessages.containsAll(List.of(message1, message2, message3)),
                            "All messages should be received");
                });
    }

    @Test
    void testPublishMessageWithCorrelationId() {
        // Given
        String testMessage = "Message with correlation ID";
        String correlationId = UUID.randomUUID().toString();

        // When
        messagePublisher.publishMessage(testMessage, correlationId);

        // Then
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<String> receivedMessages = messageSubscriber.getReceivedMessages();
                    assertTrue(receivedMessages.contains(testMessage));
                });
    }

    @Test
    void testClearReceivedMessages() {
        // Given
        messagePublisher.publishMessage("Test message");

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> !messageSubscriber.getReceivedMessages().isEmpty());

        // When
        messageSubscriber.clearReceivedMessages();

        // Then
        assertTrue(messageSubscriber.getReceivedMessages().isEmpty(),
                "Received messages should be cleared");
    }

    @Test
    void testMessageCount() {
        // Given
        messagePublisher.publishMessage("Message 1");
        messagePublisher.publishMessage("Message 2");

        // When/Then
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertEquals(2, messageSubscriber.getMessageCount());
                });
    }

}
