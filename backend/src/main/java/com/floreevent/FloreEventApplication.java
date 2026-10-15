package com.floreevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FloreEventApplication {

	public static void main(String[] args) {
		SpringApplication.run(FloreEventApplication.class, args);
	}
}
