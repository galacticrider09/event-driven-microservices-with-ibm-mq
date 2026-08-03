package com.order.payment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
@OpenAPIDefinition(info = @Info(title = "Order Payment Service API", version = "1.0", description = "API for processing payments"))
@SpringBootApplication
public class OrderPaymentApplication {
    public static void main(String[] args) { SpringApplication.run(OrderPaymentApplication.class, args); }
}
