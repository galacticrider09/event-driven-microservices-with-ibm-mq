package com.order.payment.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.order.payment.model.OrderMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJms
public class PaymentMQConfig {

    @Value("${app.mq.concurrency:1-3}")
    private String concurrency;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        return m;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper om) {
        MappingJackson2MessageConverter c = new MappingJackson2MessageConverter();

        c.setObjectMapper(om);
        c.setTargetType(MessageType.TEXT);
        c.setTypeIdPropertyName("_type");

        Map<String, Class<?>> mp = new HashMap<>();

        mp.put(
                "com.order.producer.model.OrderMessage",
                OrderMessage.class
        );
        mp.put(
                "com.order.inventory.model.OrderMessage",
                OrderMessage.class
        );

        c.setTypeIdMappings(mp);

        return c;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory cf,
            MessageConverter mc) {

        DefaultJmsListenerContainerFactory f =
                new DefaultJmsListenerContainerFactory();

        f.setConnectionFactory(cf);
        f.setMessageConverter(mc);
        f.setConcurrency(concurrency);

        f.setSessionAcknowledgeMode(
                Session.CLIENT_ACKNOWLEDGE
        );

        f.setRecoveryInterval(5000L);
        f.setErrorHandler(t -> System.err.println("Payment listener error: " + t.getMessage()));

        return f;
    }

    @Bean
    public JmsTemplate jmsTemplate(
            ConnectionFactory cf,
            MessageConverter mc) {

        JmsTemplate t = new JmsTemplate(cf);
        t.setMessageConverter(mc);

        return t;
    }
}