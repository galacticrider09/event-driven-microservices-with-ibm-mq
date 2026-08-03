package com.order.notification;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
@OpenAPIDefinition(info = @Info(title = "Order Notification Service API", version = "1.0", description = "API for sending notifications"))
@SpringBootApplication
public class OrderNotificationApplication {
    public static void main(String[] args) { SpringApplication.run(OrderNotificationApplication.class, args); }
}
