package com.kdn.opctester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.kdn.opctester")
public class OpctesterApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpctesterApplication.class, args);
	}
}