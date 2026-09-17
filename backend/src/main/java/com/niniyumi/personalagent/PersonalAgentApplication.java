package com.niniyumi.personalagent;

import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.AiProviderProperties;
import com.niniyumi.personalagent.course.infrastructure.config.CourseProperties;
import com.niniyumi.personalagent.course.infrastructure.speech.SpeechProviderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AiProviderProperties.class, CourseProperties.class, SpeechProviderProperties.class})
public class PersonalAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalAgentApplication.class, args);
	}

}
