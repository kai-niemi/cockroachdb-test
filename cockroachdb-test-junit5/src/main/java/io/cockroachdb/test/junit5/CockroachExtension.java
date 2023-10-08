package io.cockroachdb.test.junit5;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cockroachdb.test.Cockroach;
import io.cockroachdb.test.ProcessDetails;
import io.cockroachdb.test.base.StandardSteps;
import io.cockroachdb.test.base.Step;
import io.cockroachdb.test.util.OperatingSystem;

public class CockroachExtension
        implements BeforeAllCallback, AfterAllCallback,
        TestInstancePostProcessor, TestExecutionExceptionHandler {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Class<?> clazz;

        public <T> Builder withTestClass(Class<T> clazz) {
            this.clazz = clazz;
            return this;
        }

        public CockroachExtension build() {
            Cockroach cockroach = clazz.getAnnotation(Cockroach.class);
            if (cockroach == null) {
                throw new IllegalStateException(
                        "Expected @Cockroach type-level annotation for " + clazz.getName());
            }
            return new CockroachExtension(cockroach);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Cockroach cockroach;

    private final List<Step> steps = new ArrayList<>(StandardSteps.LIST);

    public CockroachExtension(Cockroach cockroach) {
        this.cockroach = cockroach;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        ProcessDetails processDetails =
                context.getStore(ExtensionContext.Namespace.GLOBAL).get("cockroachDetails", ProcessDetails.class);
        try {
            testInstance.getClass()
                    .getMethod("setProcessDetails", ProcessDetails.class)
                    .invoke(testInstance, processDetails);
        } catch (NoSuchMethodException e) {
            logger.info("Process details injection failed - no such method: " + e);
        } catch (IllegalAccessException | InvocationTargetException | SecurityException e) {
            logger.info("Process details injection failed", e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        logger.info("Bootstrap CockroachDB JUnit5 extension:\n{}",
                extensionContext.getDisplayName());

        StringWriter sw = new StringWriter();
        OperatingSystem.print(new PrintWriter(sw));
        logger.debug("Host O/S metadata:\n" + sw);

        for (Step step : steps) {
            logger.debug("Running step setup: {}", step.getClass().getName());
            step.setUp(ExtensionStoreContext.of(extensionContext), cockroach);
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        logger.error("Test execution error", throwable);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        logger.info("Teardown CockroachDB JUnit5 extension:\n{}",
                extensionContext.getDisplayName());

        Collections.reverse(steps);

        for (Step step : steps) {
            logger.debug("Running step cleanup: {}", step.getClass().getName());
            step.cleanUp(ExtensionStoreContext.of(extensionContext), cockroach);
        }
    }
}
