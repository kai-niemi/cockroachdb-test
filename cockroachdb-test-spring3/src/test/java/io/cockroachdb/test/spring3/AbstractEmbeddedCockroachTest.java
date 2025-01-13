package io.cockroachdb.test.spring3;

import javax.sql.DataSource;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import io.cockroachdb.test.Cockroach;
import io.cockroachdb.test.StartFlags;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration-test")
@SpringBootTest(classes = SpringApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Bind cockroach lifecycle to spring app events
@ContextConfiguration(loader = EmbeddedCockroachLoader.class)
@Cockroach(
        version = "v24.3.2",
        architecture = Cockroach.Architecture.arm64,
        command = Cockroach.Command.start_single_node,
        experimental = true,
        startFlags = @StartFlags(listenAddr = "localhost"),
        initSQL = {
//                "SET CLUSTER SETTING kv.raft_log.disable_synchronization_unsafe = true",
                "SET CLUSTER SETTING kv.range_merge.queue_interval = '50ms'",
                "SET CLUSTER SETTING jobs.registry.interval.gc = '30s'",
                "SET CLUSTER SETTING jobs.registry.interval.cancel = '180s'",
                "SET CLUSTER SETTING jobs.retention_time = '15s'",
                "SET CLUSTER SETTING sql.stats.automatic_collection.enabled = false",
                "SET CLUSTER SETTING kv.range_split.by_load_merge_delay = '5s'",
                "ALTER RANGE default CONFIGURE ZONE USING \"gc.ttlseconds\" = 600",
                "ALTER DATABASE system CONFIGURE ZONE USING \"gc.ttlseconds\" = 600"
        }
)
public abstract class AbstractEmbeddedCockroachTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected DataSource dataSource;

    @Test
    public void whenContextStarted_thenPrintDatabaseVersion() {
        logger.info("Connected to: {}",
                new JdbcTemplate(dataSource)
                        .queryForObject("select version()", String.class));
    }
}