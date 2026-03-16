package com.legalaid.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LegalAidBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegalAidBackendApplication.class, args);
	}

}
