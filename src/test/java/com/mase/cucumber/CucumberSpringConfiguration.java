package com.mase.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/webcbd_test?createDatabaseIfNotExist=true}",
                "spring.datasource.username=${TEST_DB_USERNAME:root}",
                "spring.datasource.password=${TEST_DB_PASSWORD:root}",
                "spring.sql.init.data-locations=classpath:data.sql",
                "spring.sql.init.mode=always"
        }
)
@ActiveProfiles("test")
class CucumberSpringConfiguration {
}
