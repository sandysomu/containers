package Anshpty.containers;


import Anshpty.containers.testcontainer.SolaceContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(SolaceContainerConfig.class)
class SolaceIntegrationTemp {

    @Autowired
    SolaceContainerConfig.SolaceTestProperties solace;

    @Test
    void shouldStartSolaceBroker() {
        System.out.println("✅ Solace running at: tcp://" + solace.host() + ":" + solace.port());
        // You can now connect with JCSMP or Spring Cloud Stream binder using these props.
    }
}


