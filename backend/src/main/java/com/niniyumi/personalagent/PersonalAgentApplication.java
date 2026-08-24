package com.niniyumi.personalagent;

import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.AiProviderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableConfigurationProperties(AiProviderProperties.class)
public class PersonalAgentApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// 外部 Tomcat 通过此入口加载与本地 main 方法相同的 Spring Boot 应用。
		return application.sources(PersonalAgentApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(PersonalAgentApplication.class, args);
	}

}
