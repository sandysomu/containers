package com.example.solace;

import com.solacesystems.jcsmp.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        // 0) Start Solace PubSub+ in a Testcontainer
        GenericContainer<?> solace = new GenericContainer<>(DockerImageName.parse("solace/solace-pubsub-standard:latest"))
                .withExposedPorts(55555, 8080, 55003) // SMF + (optional) UI/SEMP
                .withEnv("username_admin_globalaccesslevel", "admin")
                .withEnv("username_admin_password", "admin")
                .withEnv("system_scaling_maxconnectioncount", "100")
                .withSharedMemorySize(1024L * 1024L * 1024L)
                .withStartupTimeout(Duration.ofMinutes(3));

        JCSMPSession session = null;
        FlowReceiver flow = null;

        try {
            solace.start();
            String smf = "tcp://" + solace.getHost() + ":" + solace.getMappedPort(55555);
            System.out.println("Solace SMF: " + smf);

            // 1) Create JCSMP session (default VPN is 'default')
            JCSMPProperties props = new JCSMPProperties();
            props.setProperty(JCSMPProperties.HOST, smf);
            props.setProperty(JCSMPProperties.USERNAME, "admin");
            props.setProperty(JCSMPProperties.PASSWORD, "admin");
            props.setProperty(JCSMPProperties.VPN_NAME, "default");

            session = JCSMPFactory.onlyInstance().createSession(props);
            session.connect();

            // 2) Provision a durable queue and bind a topic to it
            String queueName = "q/demo";
            String topicName = "demo/in";

            Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
            EndpointProperties ep = new EndpointProperties();
            ep.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);
            ep.setPermission(EndpointProperties.PERMISSION_CONSUME);

            session.provision(queue, ep, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
            session.addSubscription(queue, topic, JCSMPSession.WAIT_FOR_CONFIRM);
            System.out.println("Provisioned queue " + queueName + " and bound topic " + topicName);

            // 3) Start a consumer on the queue
            CountDownLatch gotOne = new CountDownLatch(1);
            final StringBuilder received = new StringBuilder();

            ConsumerFlowProperties fp = new ConsumerFlowProperties();
            fp.setEndpoint(queue);
            fp.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

            flow = session.createFlow(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage msg) {
                    try {
                        if (msg instanceof TextMessage txt) {
                            received.append(txt.getText());
                            System.out.println("✅ Received: " + txt.getText());
                        } else {
                            System.out.println("ℹ️ Received non-text message: " + msg.getClass().getSimpleName());
                        }
                    } finally {
                        msg.ackMessage(); // important in CLIENT ack mode
                        gotOne.countDown();
                    }
                }
                @Override
                public void onException(JCSMPException e) {
                    e.printStackTrace();
                    gotOne.countDown();
                }
            }, fp);
            flow.start();

            // 4) Publish a message to the topic (lands in queue via topic->queue binding)
            XMLMessageProducer producer = session.getMessageProducer((e, m) -> {
                System.err.println("Publish error: " + e.getMessage());
            });
            TextMessage out = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            out.setText("hello world from testcontainer");
            producer.send(out, topic);
            System.out.println("📤 Published to topic: " + topicName);

            // 5) Await the message
            boolean ok = gotOne.await(15, TimeUnit.SECONDS);
            if (!ok) {
                throw new RuntimeException("Timed out waiting for message!");
            }

            System.out.println("🎉 Done. Message body: " + received);

        } finally {
            // 6) Clean shutdown
            if (flow != null) {
                try { flow.close(); } catch (Exception ignored) {}
            }
            if (session != null) {
                try { session.closeSession(); } catch (Exception ignored) {}
            }
            if (solace != null) {
                try { solace.stop(); } catch (Exception ignored) {}
            }
        }
    }
}




Another one
import com.solacesystems.jcsmp.*;

public final class SolaceAdmin {

  private SolaceAdmin() {}

  /**
   * Provision a durable queue (if missing) and bind a topic subscription to it.
   * Idempotent: safely ignores "already exists" for both queue and subscription.
   *
   * @param session          Connected JCSMPSession
   * @param queueName        Name of the queue to provision (e.g., "q/input")
   * @param topicSubscription Topic to bind (e.g., "demo/in" or with wildcards "demo/>")
   * @throws JCSMPException  If the broker returns an unexpected error
   */
  public static void provisionQueueAndBindTopic(JCSMPSession session,
                                                String queueName,
                                                String topicSubscription) throws JCSMPException {
    // 1) Build queue object & endpoint properties
    Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
    EndpointProperties ep = new EndpointProperties();
    ep.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);   // single consumer; change to NONEXCLUSIVE if needed
    ep.setPermission(EndpointProperties.PERMISSION_CONSUME);     // allow consuming

    // 2) Provision the queue (ignore if it already exists)
    session.provision(queue, ep, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

    // 3) Bind the topic subscription to the queue (so publishes to the topic get enqueued)
    Topic topic = JCSMPFactory.onlyInstance().createTopic(topicSubscription);
    try {
      session.addSubscription(queue, topic, JCSMPSession.WAIT_FOR_CONFIRM);
    } catch (JCSMPErrorResponseException e) {
      // If it already exists, ignore; otherwise rethrow
      if (!isAlreadyExists(e)) {
        throw e;
      }
    }
  }

  /**
   * Basic check for "already exists" scenarios from the broker.
   * Different broker versions may use different subcodes; keep this tolerant.
   */
  private static boolean isAlreadyExists(JCSMPErrorResponseException e) {
    final int subcode = e.getSubcodeEx();
    // Known subcodes for "already exists" differ by operation; keep lenient.
    // Common ones include: JCSMPErrorResponseSubcode.ALREADY_EXISTS, SUBSCRIPTION_ALREADY_PRESENT, etc.
    // Fall back to message contains check as a last resort.
    return subcode == JCSMPErrorResponseSubcode.ALREADY_EXISTS
        || subcode == JCSMPErrorResponseSubcode.SUBSCRIPTION_ALREADY_PRESENT
        || (e.getMessage() != null && e.getMessage().toLowerCase().contains("already exist"));
  }
}

