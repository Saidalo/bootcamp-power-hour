package com.bootcamp.powerhour;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"elering.import-on-startup=false",
		"elering.import-cron=-"
})
class PowerhourApplicationTests {

	@Test
	void contextLoads() {
	}

}
