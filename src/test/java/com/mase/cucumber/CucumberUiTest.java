package com.mase.cucumber;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.junit.platform.engine.Cucumber;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@Cucumber
class CucumberUiTest {

    @Test
    void uiFeatureIsOnClasspath() {
        assertNotNull(
                CucumberUiTest.class.getClassLoader().getResource("features/ui.feature"),
                "Expected UI feature to be available on the classpath");
    }

    @Test
    void uiFeatureHasContent() throws IOException {
        var stream = CucumberUiTest.class.getClassLoader().getResourceAsStream("features/ui.feature");
        assertNotNull(stream, "UI feature resource missing");
        try (stream) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains("Feature:"), "UI feature should declare a Feature");
        }
    }

    @Test
    void uiStepsAreLoadable() {
        assertNotNull(new UiSteps(), "UI steps should be instantiable");
    }
}
