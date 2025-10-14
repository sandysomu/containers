package Anshpty.containers.testcontainer;

import com.solacesystems.jcsmp.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class SolaceTestSupport {

    private static final DockerImageName SOLACE_IMAGE =
            DockerImageName.parse("solace/solace-pubsub-standard:latest");

    private static GenericContainer<?> container;
    private static JCSMPSession session;

    public static void startContainer() throws Exception {
        if (container == null) {
            container = new GenericContainer<>(SOLACE_IMAGE)
                    .withExposedPorts(55555, 8080, 8008)
                    .withEnv("username_admin_globalaccesslevel", "admin")
                    .withEnv("username_admin_password", "admin")
                    .withReuse(true);
            container.start();

            System.setProperty("solace.host", container.getHost());
            System.setProperty("solace.port", container.getMappedPort(55555).toString());
            System.out.println("✅ Solace container started at tcp://" + container.getHost() + ":" + container.getMappedPort(55555));
        }
    }

    public static JCSMPSession createSession() throws Exception {
        if (session == null) {
            JCSMPProperties props = new JCSMPProperties();
            props.setProperty(JCSMPProperties.HOST, "tcp://" + container.getHost() + ":" + container.getMappedPort(55555));
            props.setProperty(JCSMPProperties.USERNAME, "admin");
            props.setProperty(JCSMPProperties.PASSWORD, "admin");
            props.setProperty(JCSMPProperties.VPN_NAME, "default");
            session = JCSMPFactory.onlyInstance().createSession(props);
            session.connect();
            System.out.println("✅ Connected to Solace session");
        }
        return session;
    }

//    public static void createTopic(String topicName) throws Exception {
//        JCSMPSession session = createSession();
//        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
//
//        // Provision the topic endpoint
//        EndpointProperties ep = new EndpointProperties();
//        ep.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
//        ep.setPermission(EndpointProperties.PERMISSION_CONSUME);
//        ep.setQuotaMB(100);
//        session.provision(JCSMPFactory.onlyInstance().createTopicEndpoint(topic.getName()), ep, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
//
//        System.out.println("✅ Topic created: " + topicName);
//    }
    public static void createTopic(String topicName) throws Exception {
        JCSMPSession session = createSession();
        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);

        // Just add subscription (topics don’t need provisioning)
        session.addSubscription(topic);
        System.out.println("✅ Subscribed to topic: " + topicName);
    }

    public static void publishMessage(String topicName, String messageText) throws Exception {
        JCSMPSession session = createSession();
        XMLMessageProducer producer = session.getMessageProducer(
                new JCSMPStreamingPublishEventHandler() {
                    public void responseReceived(String messageID) {
                        System.out.println("✔️ Message acknowledged, ID: " + messageID);
                    }

                    public void handleError(String messageID, JCSMPException e, long timestamp) {
                        System.err.println("❌ Publish error: " + e.getMessage());
                    }
                });

        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        msg.setText(messageText);
        producer.send(msg, JCSMPFactory.onlyInstance().createTopic(topicName));
        System.out.println("📤 Published message to topic: " + topicName);
    }

    public static void stopContainer() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }
}