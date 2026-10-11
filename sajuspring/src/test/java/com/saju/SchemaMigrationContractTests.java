package com.saju;

import com.saju.entity.UserEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Offline mapping check. No Spring context, DataSource, or database access. */
class SchemaMigrationContractTests {
    @Test
    void usersMigrationMatchesHibernateMetadata() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V1__initial_saju_schema.sql"));
        String users = sql.substring(sql.indexOf("CREATE TABLE users"), sql.indexOf("CREATE TABLE client_services"))
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")
                .replaceAll("\\s*,\\s*", ",").replace("bit(1)", "bit");
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", new org.hibernate.dialect.MySQLDialect(
                        org.hibernate.dialect.DatabaseVersion.make(8, 4)))
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        try {
            var metadata = new MetadataSources(registry).addAnnotatedClass(UserEntity.class)
                    .getMetadataBuilder()
                    .applyPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy())
                    .build();
            var table = metadata.getEntityBinding(UserEntity.class.getName()).getTable();
            assertEquals("users", table.getName());
            assertEquals(11, table.getColumns().size());
            for (var column : table.getColumns()) {
                String type = column.getSqlType(metadata).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")
                        .replaceAll("\\s*,\\s*", ",").replace("bit(1)", "bit").replace("enum (", "enum(");
                String expected = column.getName() + " " + type + (column.isNullable() ? " null" : " not null");
                assertTrue(users.contains(expected), () -> "Migration mismatch: " + expected);
            }
            assertTrue(users.contains("id bigint not null auto_increment"));
            assertTrue(users.contains("primary key (id)"));
            assertTrue(users.contains("unique (email)"));
            assertFalse(sql.matches("(?is).*\\b(DROP|TRUNCATE|DELETE|UPDATE|INSERT)\\s+(TABLE|FROM|INTO|users)\\b.*"));
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
