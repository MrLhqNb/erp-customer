package com.jumbosoft.erpcustomer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.jumbosoft.erpcustomer.config")
public class ErpCustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpCustomerApplication.class, args);
    }
}
