package com.instagram.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


@SpringBootApplication
@EnableCaching
public class InstagramBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(InstagramBackendApplication.class, args);
	}

}
