package Anshpty.containers.testcontainer;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class SolaceContainerConfig {

    private static final DockerImageName SOLACE_IMAGE =
            DockerImageName.parse("solace/solace-pubsub-standard:latest");

    @Bean(name = "solaceContainer", destroyMethod = "stop")
    public GenericContainer<?> solaceContainer() {
        GenericContainer<?> container = new GenericContainer<>(SOLACE_IMAGE)
                .withExposedPorts(55555, 8080, 8008)
                .withEnv("username_admin_globalaccesslevel", "admin")
                .withEnv("username_admin_password", "admin")
                .withReuse(true);

        container.start();

        System.setProperty("solace.host", container.getHost());
        System.setProperty("solace.port", container.getMappedPort(55555).toString());
        System.setProperty("solace.webui", "http://" + container.getHost() + ":" + container.getMappedPort(8080));

        return container;
    }

    @Bean
    @DependsOn("solaceContainer")
    public SolaceTestProperties solaceTestProperties(GenericContainer<?> solaceContainer) {
        String host = solaceContainer.getHost();
        int port = solaceContainer.getMappedPort(55555);
        return new SolaceTestProperties(host, port);
    }

    public record SolaceTestProperties(String host, int port) {}
}

