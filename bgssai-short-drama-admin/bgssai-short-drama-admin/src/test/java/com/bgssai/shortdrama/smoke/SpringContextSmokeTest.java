package com.bgssai.shortdrama.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 4 启动冒烟测试（管理端模块）：加载完整应用上下文，验证所有 Bean 在
 * SB4 下正常装配（Druid 数据源、MyBatis 4.x、PageHelper 4.x、AOP 切面、Jackson 2
 * 消息转换器等）。上下文成功加载即视为通过。
 * <p>
 * 该测试需要可连通的数据库，默认不在离线 / CI 环境运行。本地联调时配好数据库连接后，
 * 设置环境变量 {@code BGSS_SMOKE_DB=true} 再执行：
 * <pre>
 *   BGSS_SMOKE_DB=true mvn -Dtest=SpringContextSmokeTest test
 * </pre>
 * 未设置该环境变量时，整个测试类被 JUnit 跳过（不会加载上下文，离线构建不受影响）。
 */
@EnabledIfEnvironmentVariable(named = "BGSS_SMOKE_DB", matches = "true")
@SpringBootTest
class SpringContextSmokeTest {

    @Test
    void contextLoads() {
        // 上下文成功加载即通过。
    }
}
