package testing;

import com.solacesystems.jcsmp.*;
import java.nio.charset.StandardCharsets;

/**
 * Tiny helper for provisioning queues and simple publish/consume in Solace JCSMP.
 */
public class SolaceAdmin implements AutoCloseable {

    private final JCSMPSession session;

    public SolaceAdmin(JCSMPSession session) {
        this.session = session;
    }

    /** Provision a durable queue if it doesn’t already exist. */
    public void ensureQueue(String queueName) throws JCSMPException {
        final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
        final EndpointProperties props = new EndpointProperties();
        props.setPermission(EndpointProperties.PERMISSION_CONSUME);
        props.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
        try {
            session.provision(queue, props, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
        } catch (JCSMPErrorResponseException e) {
            if (!e.getMessage().contains("ALREADY_EXISTS")) {
                throw e;
            }
        }
    }

//    /** Publish text message to a queue. */
//    public void publishText(String queueName, String text) throws JCSMPException {
//        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
//        XMLMessageProducer producer = session.getMessageProducer((XMLMessageProducer.EventHandler) null);
//        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
//        msg.setText(text);
//        producer.send(msg, queue);
//    }

    /** Publish raw bytes to a queue. */
//    public void publishBytes(String queueName, byte[] payload) throws JCSMPException {
//        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
//        XMLMessageProducer producer = session.getMessageProducer((XMLMessageProducer.EventHandler) null);
//        BytesMessage msg = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
//        msg.setData(payload);
//        producer.send(msg, queue);
//    }

    /** Consume a single message from a queue (blocking). */
    public byte[] consumeOne(String queueName, long timeoutMillis) throws JCSMPException {
        ConsumerFlowProperties props = new ConsumerFlowProperties();
        props.setEndpoint(JCSMPFactory.onlyInstance().createQueue(queueName));
        props.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        FlowReceiver flow = session.createFlow(new XMLMessageListener() {
            @Override public void onReceive(BytesXMLMessage msg) {}
            @Override public void onException(JCSMPException e) { e.printStackTrace(); }
        }, props);

        flow.start();
        try {
            BytesXMLMessage msg = flow.receive((int) timeoutMillis);
            if (msg == null) return null;
            byte[] data = extract(msg);
            msg.ackMessage();
            return data;
        } finally {
            flow.stop();
            flow.close();
        }
    }

    /** Extract payload bytes from a JCSMP message. */
    private static byte[] extract(BytesXMLMessage msg) {
        if (msg instanceof BytesMessage bm) {
            byte[] data = bm.getData();
            return data != null ? data : new byte[0];
        }
        if (msg instanceof TextMessage tm) {
            String text = tm.getText();
            return text != null ? text.getBytes(StandardCharsets.UTF_8) : new byte[0];
        }
        return new byte[0];
    }

    /** Bind a topic to a queue (for topic-to-queue mapping). */
    public void bindTopicToQueue(String topic, String queueName) throws JCSMPException {
        final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
        final Topic t = JCSMPFactory.onlyInstance().createTopic(topic);
        session.addSubscription(queue, t, JCSMPSession.WAIT_FOR_CONFIRM);
    }


    @Override
    public void close() {
        // session lifecycle usually managed by caller
    }
}
