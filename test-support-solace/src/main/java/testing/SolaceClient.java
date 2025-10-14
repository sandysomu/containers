package testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solacesystems.jcsmp.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/** Minimal publish/consume helpers for tests. */
public class SolaceClient implements AutoCloseable {
    private final JCSMPSession session;
    private final ObjectMapper mapper = new ObjectMapper();


    public SolaceClient(JCSMPSession session) { this.session = session; }


    public void publishJsonToQueue(String queueName, Object payload) throws Exception {
        byte[] bytes = mapper.writeValueAsBytes(payload);
        publishBytesToQueue(queueName, bytes);
    }


    public void publishTextToQueue(String queueName, String text) throws Exception {
        publishBytesToQueue(queueName, text.getBytes(StandardCharsets.UTF_8));
    }


    public void publishBytesToQueue(String queueName, byte[] bytes) throws Exception {
        final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
        final XMLMessageProducer prod = session.getMessageProducer((JCSMPStreamingPublishEventHandler) null);
        final BytesXMLMessage msg = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
        msg.writeAttachment(bytes);
        prod.send(msg, queue);
    }


    /**
     * Pull a single message from a queue with timeout; returns body bytes or null if none.
     */
    public byte[] consumeOne(String queueName, long timeoutMillis) throws Exception {
        ConsumerFlowProperties fp = new ConsumerFlowProperties();
        fp.setEndpoint(JCSMPFactory.onlyInstance().createQueue(queueName));
        fp.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        FlowReceiver flow = session.createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                // no-op; using blocking receive
            }

            @Override
            public void onException(JCSMPException e) {
                e.printStackTrace();
            }
        }, fp);

        flow.start();
        try {
            BytesXMLMessage msg = flow.receive((int) timeoutMillis);
            if (msg == null) return null;
            byte[] data = extractBytes(msg);
            msg.ackMessage();
            return data;
        } finally {
            flow.stop();
            flow.close();
        }
    }

    private static byte[] extractBytes(BytesXMLMessage msg) {
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




    /** Subscribe to a queue and capture the next message (push style). */
    public byte[] awaitNext(String queueName, long timeoutMillis) throws Exception {
        ConsumerFlowProperties fp = new ConsumerFlowProperties();
        fp.setEndpoint(JCSMPFactory.onlyInstance().createQueue(queueName));
        fp.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> ref = new AtomicReference<>();

        FlowReceiver flow = session.createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                try {
                    ref.set(extractBytes(msg));   // <- use helper below
                    msg.ackMessage();
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            }
            @Override
            public void onException(JCSMPException e) {
                // optional: log
            }
        }, fp);

        flow.start();
        try {
            boolean ok = latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            return ok ? ref.get() : null;
        } finally {
            flow.stop();
            flow.close();
        }
    }


    @Override public void close() { /* session owned by caller */ }
}
