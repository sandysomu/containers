package Anshpty.containers.service;
import com.solacesystems.jcsmp.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class MessageSubscriber {
    @Value("${app.solace.topic}")
    private String topicName;

    @Autowired
    private JCSMPProperties jcsmpProperties;

    private JCSMPSession session;
    private XMLMessageConsumer consumer;

    @Getter
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() throws Exception {
        session = JCSMPFactory.onlyInstance().createSession(jcsmpProperties);
        session.connect();

        final Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);

        consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage message) {
                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    String text = textMessage.getText();
                    String correlationId = message.getCorrelationId();

                    log.info("Received message on topic '{}': {} (correlationId: {})",
                            message.getDestination().getName(), text, correlationId);

                    receivedMessages.add(text);
                    processMessage(text, correlationId);
                }
            }

            @Override
            public void onException(JCSMPException e) {
                log.error("Consumer received exception", e);
            }
        });

        session.addSubscription(topic);
        consumer.start();

        log.info("Solace subscriber initialized and listening on topic: {}", topicName);
    }

    private void processMessage(String message, String correlationId) {
        log.debug("Processing message: {} with correlationId: {}", message, correlationId);
        // Add your business logic here
    }

    public void clearReceivedMessages() {
        receivedMessages.clear();
        log.debug("Received messages cleared");
    }

    public int getMessageCount() {
        return receivedMessages.size();
    }

    @PreDestroy
    public void cleanup() {
        if (consumer != null) {
            consumer.close();
            log.info("Solace consumer closed");
        }
        if (session != null) {
            session.closeSession();
            log.info("Solace session closed");
        }
    }
}
