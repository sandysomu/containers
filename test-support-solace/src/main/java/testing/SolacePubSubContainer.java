package testing;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


import java.time.Duration;


/**
 * Solace PubSub+ Standard container with sane defaults for tests.
 * Exposes SMF (55554) for non-TLS clients and waits for broker readiness.
 */
public class SolacePubSubContainer extends GenericContainer<SolacePubSubContainer> {
    public static final int SMF_PORT = 55554; // non-TLS SMF
    public static final int SEMP_PORT = 8080; // HTTP mgmt (optional)


    public SolacePubSubContainer() {
        super(DockerImageName.parse("solace/solace-pubsub-standard:latest"));


// Minimal env setup: admin/admin on default VPN
        withEnv("username_admin_globalaccesslevel", "admin");
        withEnv("username_admin_password", "admin");
        withEnv("SEMP_ENABLE", "true");
        withEnv("SEMP_OVER_SSL", "false");
        withEnv("PUBSUB_SECURE_INTERFACE_ENABLE", "false");
        withEnv("service_enable", "smf"); // enable SMF
        withEnv("logging_level", "INFO");


        addExposedPorts(SMF_PORT, SEMP_PORT);


// Solace takes some time; wait for log & listening port
        waitingFor(
                Wait.forListeningPort()
                        .withStartupTimeout(Duration.ofMinutes(3))
        ).waitingFor(
                Wait.forLogMessage(".*Primary Virtual Router.*active.*\n", 1)
                        .withStartupTimeout(Duration.ofMinutes(3))
        );


        withReuse(false);
    }


    public String smfHost() {
        return getHost();
    }


    public int smfPort() {
        return getMappedPort(SMF_PORT);
    }


    /** tcp://host:port for SMF */
    public String smfEndpoint() {
        return "tcp://" + smfHost() + ":" + smfPort();
    }
}