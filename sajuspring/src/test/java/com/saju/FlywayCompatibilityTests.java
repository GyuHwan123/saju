package com.saju;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.resource.StringResource;
import org.flywaydb.database.mysql.MySQLParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Tests library integration without connecting to any database or running SQL. */
class FlywayCompatibilityTests {
    @Test
    void bootAutoConfigurationSupportsFlywayWithMigrationIntercepted() {
        DataSource dataSource = mock(DataSource.class);
        AtomicBoolean initialized = new AtomicBoolean();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
                .withBean(DataSource.class, () -> dataSource)
                .withBean(FlywayMigrationStrategy.class, () -> flyway -> initialized.set(true))
                .withPropertyValues("spring.flyway.enabled=true", "spring.flyway.clean-disabled=true",
                        "spring.flyway.baseline-on-migrate=false", "spring.flyway.validate-on-migrate=true",
                        "spring.flyway.locations=classpath:db/migration")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(Flyway.class);
                    var configuration = context.getBean(Flyway.class).getConfiguration();
                    assertThat(configuration.isCleanDisabled()).isTrue();
                    assertThat(configuration.isBaselineOnMigrate()).isFalse();
                    assertThat(configuration.isValidateOnMigrate()).isTrue();
                    assertThat(initialized).isTrue();
                    verifyNoInteractions(dataSource);
                });
    }

    @Test
    void mysqlParserRecognizesOnlyFiveCreateTableStatements() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V1__initial_saju_schema.sql"));
        var parser = new MySQLParser(Flyway.configure(), new ParsingContext());
        int count = 0;
        try (var statements = parser.parse(new StringResource(sql))) {
            while (statements.hasNext()) {
                String statement = statements.next().getSql()
                        .replaceAll("(?m)^\\s*--[^\\r\\n]*", "").stripLeading();
                assertThat(statement).startsWith("CREATE TABLE ");
                count++;
            }
        }
        assertThat(count).isEqualTo(5);
    }
}
