package com.somosayni.asistente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.somosayni.asistente", "com.somosayni.shared"})
public class AsistenteApplication {
    public static void main(String[] args) {
        SpringApplication.run(AsistenteApplication.class, args);
    }
}
