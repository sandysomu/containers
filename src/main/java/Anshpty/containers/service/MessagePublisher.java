package Anshpty.containers.service;

import com.solacesystems.jcsmp.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessagePublisher {
    @Value("${app.solace.topic}")
    private String topicName;

    @Autowired
    private JCSMPProperties jcsmpProperties;

    private JCSMPSession session;
    private XMLMessageProducer producer;

    @PostConstruct
    public void init() throws Exception {
        session = JCSMPFactory.onlyInstance().createSession(jcsmpProperties);
        session.connect();

        producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
            @Override
            public void responseReceivedEx(Object key) {
                log.debug("Producer received response for msg: {}", key);
            }

            @Override
            public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
                log.error("Producer error for msg: {}", key, cause);
            }
        });

        log.info("Solace publisher initialized successfully on topic: {}", topicName);
    }

    public void publishMessage(String message) {
        publishMessage(message, null);
    }

    public void publishMessage(String message, String correlationId) {
        try {
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(message);

            if (correlationId != null) {
                msg.setCorrelationId(correlationId);
            }

            producer.send(msg, topic);
            log.info("Published message to topic '{}': {} (correlationId: {})",
                    topicName, message, correlationId);
        } catch (JCSMPException e) {
            log.error("Error publishing message", e);
            throw new RuntimeException("Failed to publish message: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (producer != null) {
            producer.close();
            log.info("Solace producer closed");
        }
        if (session != null) {
            session.closeSession();
            log.info("Solace session closed");
        }
    }

}
