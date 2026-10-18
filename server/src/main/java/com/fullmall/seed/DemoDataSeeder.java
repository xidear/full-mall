package com.fullmall.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Profile({"dev", "demo"})
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DemoDataSeeder(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long.class);
        if (productCount == null) {
            throw new IllegalStateException("Product count query returned null during demo seeding");
        }
        if (productCount == 0) {
            execute("db/demo/seed.sql");
        } else {
            log.info("Demo product seed skipped because product table already has {} rows", productCount);
        }

        Long staffCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM staff_user", Long.class);
        if (staffCount == null) {
            throw new IllegalStateException("Staff count query returned null during demo seeding");
        }
        if (staffCount == 0) {
            execute("db/demo/staff.sql");
        }
        execute("db/demo/images.sql");
        log.info("Demo seed completed");
    }

    private void execute(String classpathScript) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource(classpathScript));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }
}