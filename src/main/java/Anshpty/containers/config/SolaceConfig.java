package Anshpty.containers.config;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolaceConfig {

    @Value("${solace.java.host}")
    private String host;

    @Value("${solace.java.msg-vpn}")
    private String vpn;

    @Value("${solace.java.client-username}")
    private String username;

    @Value("${solace.java.client-password}")
    private String password;

    @Bean
    public JCSMPProperties jcsmpProperties() {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        properties.setProperty(JCSMPProperties.USERNAME, username);
        properties.setProperty(JCSMPProperties.PASSWORD, password);
        return properties;
    }
}