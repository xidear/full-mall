package com.fullmall.seed;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DemoDataSeederTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DataSource dataSource = mock(DataSource.class);

    @Test
    void refusesToSeedWhenAppModeIsProd() {
        DemoDataSeeder seeder = new DemoDataSeeder(jdbcTemplate, dataSource, "prod");

        assertThatThrownBy(seeder::seed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.mode=prod");
    }

    @Test
    void prodCheckIsCaseInsensitive() {
        DemoDataSeeder seeder = new DemoDataSeeder(jdbcTemplate, dataSource, "PROD");

        assertThatThrownBy(seeder::seed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.mode=prod");
    }

    @Test
    void allowsNonProdModeToContinueIntoSeeding() {
        DemoDataSeeder seeder = new DemoDataSeeder(jdbcTemplate, dataSource, "dev");

        // app.mode 守卫放行后才会访问数据库（此处仓储未打桩，因此后续必然失败）；
        // 断言失败原因不是 prod 守卫，证明确实越过了守卫。
        assertThatThrownBy(seeder::seed)
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("app.mode=prod"));
    }
}