package org.example.carpet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "org.example.carpet.client")
public class CarpetApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarpetApplication.class, args);
    }
}
