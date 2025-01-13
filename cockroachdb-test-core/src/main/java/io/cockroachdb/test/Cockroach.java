package io.cockroachdb.test;

import io.cockroachdb.test.download.DefaultURLResolver;

import java.lang.annotation.*;

/**
 * Describes the CockroachDB version to download, install and launch
 * as part of the test lifecycle.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Cockroach {
    /**
     * Defines the version to download.
     * @see <a href="https://www.cockroachlabs.com/docs/releases/.">releases</a>
     */
    String version() default "v24.3.2";

    /**
     * Defines the base URL of the binaries http repository.
     */
    String baseURL() default "https://binaries.cockroachdb.com";

    /**
     * Defines the CPU architecture for the binary.
     */
    Architecture architecture() default Architecture.amd64;

    enum Architecture {
        amd64,
        arm64;
    }

    /**
     * Use experimental binaries for os/x platform.
     */
    boolean experimental() default false;

    /**
     * Defines the download URL resolver class.
     */
    Class<? extends URLResolver> binaryResolver() default DefaultURLResolver.class;
    /**
     * Determines if downloaded binaries should be cached locally or not between test runs.
     * Uses conditional HTTP cache header directives if retained.
     */
    boolean cacheBinary() default true;

    /**
     * Defines the CockroachDB command to use when running the process.
     */
    Command command() default Command.demo;

    enum Command {
        start_single_node,
        demo
    }

    /**
     * Defines flags for the start and start_single_node commands.
     */
    StartFlags startFlags() default @StartFlags();

    /**
     * Defines flags for the demo command.
     */
    DemoFlags demoFlags() default @DemoFlags(global = true, nodes = 9);

    /**
     * Defines a list of SQL statements to execute for initialization.
     */
    String[] initSQL() default {};

    /**
     * Maximum duration in seconds to wait for node startup before giving up.
     */
    int nodeStartupWaitSeconds() default 15;

    /**
     * Minimum duration in seconds to wait for node shutdown.
     */
    int nodeShutdownWaitSeconds() default 5;

    /**
     * If non-empty, redirect Cockroach process stderr and stdout to the given file in platforms temp dir.
     */
    String redirectProcessOutputToFile() default "";
}
