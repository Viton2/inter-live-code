package co.inter.piggies;


import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import co.inter.piggies.support.AbstractIntegrationTest;
import jakarta.inject.Inject;

@MicronautTest
class InterLiveCodeTest extends AbstractIntegrationTest {

    @Inject
    EmbeddedApplication<?> application;

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

}
