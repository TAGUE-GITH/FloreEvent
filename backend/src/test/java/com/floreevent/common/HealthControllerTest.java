package com.floreevent.common;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.floreevent.IntegrationTest;

class HealthControllerTest extends IntegrationTest {

	@Test
	void theHealthCheckIsPublicAndReachesTheDatabase() throws Exception {
		getAs(null, "/api/health")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void everythingElseRequiresAuthentication() throws Exception {
		getAs(null, "/api/items").andExpect(status().isUnauthorized());
	}
}
