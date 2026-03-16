package com.mase.karate;

import com.intuit.karate.junit5.Karate;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

// Karate runner that boots the Spring application on a random port.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
