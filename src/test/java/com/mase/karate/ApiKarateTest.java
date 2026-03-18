package com.mase.karate;

import com.intuit.karate.junit5.Karate;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

// Karate runner that boots the Spring application on a random port.
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiKarateTest {

    @LocalServerPort
    private int port;

    @Karate.Test
    // Executes all Karate features under com/mase/karate.
    Karate runAll() {
        return Karate.run("classpath:com/mase/karate")
                .systemProperty("karate.port", String.valueOf(port));
    }
}
