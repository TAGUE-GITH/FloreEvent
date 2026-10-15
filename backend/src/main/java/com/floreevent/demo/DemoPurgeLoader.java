package com.floreevent.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** En mode reel (APP_DEMO=false), supprime au demarrage les donnees de demonstration qui resteraient en base. */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "false", matchIfMissing = true)
public class DemoPurgeLoader implements ApplicationRunner {

	private final DemoPurge purge;

	public DemoPurgeLoader(DemoPurge purge) {
		this.purge = purge;
	}

	@Override
	public void run(ApplicationArguments args) {
		purge.purge();
	}
}
