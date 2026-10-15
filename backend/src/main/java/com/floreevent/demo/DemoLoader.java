package com.floreevent.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Au demarrage, en mode demonstration (APP_DEMO=true), charge les donnees fictives si elles n'existent pas encore. */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoLoader implements ApplicationRunner {

	private final DemoService demo;
	private final boolean photos;

	public DemoLoader(DemoService demo, @Value("${app.demo.photos}") boolean photos) {
		this.demo = demo;
		this.photos = photos;
	}

	@Override
	public void run(ApplicationArguments args) {
		demo.seed(photos);
	}
}
