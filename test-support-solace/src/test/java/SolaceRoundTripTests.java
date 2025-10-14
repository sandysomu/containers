import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPSession;
import org.junit.jupiter.api.*;
import testing.SolaceAdmin;
import testing.SolaceClient;
import testing.SolaceTestSupport;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SolaceRoundTripTests {

    private static SolaceTestSupport support;
    private static JCSMPSession session;
    private static SolaceAdmin admin;
    private static SolaceClient client;

    private static final String QUEUE = "q.it.demo";

    @BeforeAll
    static void boot() throws JCSMPException {
        System.out.println("Starting Solace container...");

        support = new SolaceTestSupport();
        support.start();

        session = support.session();
        admin = support.admin();
        client = support.client();

        // Provision the queue before tests
        admin.ensureQueue(QUEUE);
        
        System.out.println("Solace container started successfully!");
        System.out.println("SMF Endpoint: " + support.container().smfEndpoint());
    }

    @AfterAll
    static void shutdown() {
        System.out.println("Shutting down Solace container...");
        if (support != null) {
            support.close();
        }
        System.out.println("Solace container stopped.");
    }

    @Test
    void publishAndConsume_print() throws Exception {
        System.out.println("Publishing to queue, test method ... ");
        System.out.println("Test passed - basic print test completed.");
    }
    
    @Test
    void publishText_thenConsume_backFromSameQueue() throws Exception {
        System.out.println("Running publishText_thenConsume_backFromSameQueue test...");
        
        String payload = "hello-solace";
        client.publishTextToQueue(QUEUE, payload);
        System.out.println("Published message: " + payload);

        byte[] bytes = client.consumeOne(QUEUE, 10_000);
        assertNotNull(bytes, "No message received within timeout");
        
        String received = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Received message: " + received);
        
        assertEquals(payload, received);
        System.out.println("Test passed!");
    }

    @Test
    void publishBytes_thenConsume_backFromSameQueue() throws Exception {
        System.out.println("Running publishBytes_thenConsume_backFromSameQueue test...");
        
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        client.publishBytesToQueue(QUEUE, payload);
        System.out.println("Published byte array of length: " + payload.length);

        byte[] bytes = client.consumeOne(QUEUE, 10_000);
        assertNotNull(bytes, "No message received within timeout");
        System.out.println("Received byte array of length: " + bytes.length);
        
        assertArrayEquals(payload, bytes);
        System.out.println("Test passed!");
    }
}