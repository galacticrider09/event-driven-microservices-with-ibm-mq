package com.order.inventory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
@OpenAPIDefinition(info = @Info(title = "Order Inventory Service API", version = "1.0", description = "API for managing order inventory"))
@SpringBootApplication
public class OrderInventoryApplication {
    public static void main(String[] args) { SpringApplication.run(OrderInventoryApplication.class, args); }


}
