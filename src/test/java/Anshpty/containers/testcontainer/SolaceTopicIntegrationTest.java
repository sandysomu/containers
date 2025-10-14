package Anshpty.containers.testcontainer;

//import com.example.testcontainers.SolaceTestSupport;
import com.solacesystems.jcsmp.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class SolaceTopicIntegrationTest {

    @BeforeAll
    static void setup() throws Exception {
        SolaceTestSupport.startContainer();
        SolaceTestSupport.createTopic("test/topic/sample");
    }

    @AfterAll
    static void cleanup() {
        SolaceTestSupport.stopContainer();
    }

//    @Test
//    void testPublishAndConsumeMessage() throws Exception {
//        JCSMPSession session = SolaceTestSupport.createSession();
//        final String topicName = "test/topic/sample";
//
//        // Subscribe to the topic
//        final CountDownLatch latch = new CountDownLatch(1);
//        session.addSubscription(JCSMPFactory.onlyInstance().createTopic(topicName));
//        XMLMessageConsumer consumer = session.getMessageConsumer((XMLMessageListener) (msg -> {
//            if (msg instanceof TextMessage textMessage) {
//                System.out.println("📥 Received message: " + textMessage.getText());
//                latch.countDown();
//            }
//        }));
//
//        consumer.start();
//        SolaceTestSupport.publishMessage(topicName, "Hello from Testcontainers!");
//
//        latch.await();
//        consumer.close();
//    }
    @Test
    void testPublishAndConsumeMessage() throws Exception {
        JCSMPSession session = SolaceTestSupport.createSession();
        final String topicName = "test/topic/sample";

        // Subscribe before starting the consumer
        session.addSubscription(JCSMPFactory.onlyInstance().createTopic(topicName));

        final CountDownLatch latch = new CountDownLatch(1);

        XMLMessageListener listener = new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                try {
                    if (msg instanceof TextMessage) {
                        String text = ((TextMessage) msg).getText();
                        System.out.println("📥 Received message: " + text);
                        latch.countDown();
                    } else {
                        System.out.println("📥 Received non-text message: " + msg.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("❌ Consumer exception: " + e.getMessage());
            }
        };

        XMLMessageConsumer consumer = session.getMessageConsumer(listener);
        try {
            consumer.start();

            // publish after the consumer is started
            SolaceTestSupport.publishMessage(topicName, "Hello from Testcontainers!");

            // avoid an infinite wait in tests
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Did not receive message within timeout");
            }
        } finally {
            consumer.close();
        }
    }
}