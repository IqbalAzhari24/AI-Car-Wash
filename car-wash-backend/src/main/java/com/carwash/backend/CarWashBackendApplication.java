package com.carwash.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point. Lives in the root package {@code com.carwash.backend}
 * so Spring Boot's default component/entity/repository scanning covers every
 * sub-package (config, controller, service, repository, entity, dto).
 */
@SpringBootApplication
public class CarWashBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CarWashBackendApplication.class, args);
	}

}
