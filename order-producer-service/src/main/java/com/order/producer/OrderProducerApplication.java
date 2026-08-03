package com.order.producer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(title = "Order Producer Service API", version = "1.0", description = "API for producing orders to MQ"))
@SpringBootApplication
@EnableScheduling
public class OrderProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderProducerApplication.class, args);
    }
}
