package testing;
import com.solacesystems.jcsmp.*;

public class SolaceTestSupport implements AutoCloseable {
    private final SolacePubSubContainer container = new SolacePubSubContainer();
    private JCSMPSession session;

    public SolaceTestSupport start() throws JCSMPException {
        container.start();
        JCSMPProperties props = new JCSMPProperties();
        props.setProperty(JCSMPProperties.HOST, container.smfEndpoint());
        props.setProperty(JCSMPProperties.VPN_NAME, "default");
        props.setProperty(JCSMPProperties.USERNAME, "admin");
        props.setProperty(JCSMPProperties.PASSWORD, "admin");
        session = JCSMPFactory.onlyInstance().createSession(props);
        session.connect();
        return this;
    }

    public SolacePubSubContainer container() { return container; }
    public JCSMPSession session() { return session; }
    public SolaceAdmin admin() { return new SolaceAdmin(session); }
    public SolaceClient client() { return new SolaceClient(session); }

    @Override public void close() {
        try { if (session != null) session.closeSession(); } catch (Exception ignored) {}
        try { container.stop(); } catch (Exception ignored) {}
    }
}
