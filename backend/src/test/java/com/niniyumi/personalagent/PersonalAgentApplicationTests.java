package com.niniyumi.personalagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import static org.assertj.core.api.Assertions.assertThat;

class PersonalAgentApplicationTests {

	@Test
	void applicationTypeExists() {
		assertThat(PersonalAgentApplication.class).isNotNull();
	}

	@Test
	void applicationUsesExecutableJarBootstrapOnly() {
		assertThat(SpringBootServletInitializer.class.isAssignableFrom(PersonalAgentApplication.class))
				.isFalse();
	}

}
