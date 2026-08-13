package com.bgssai.shortdrama.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.bgssai.shortdrama.health.dto.ComponentHealth;
import com.bgssai.shortdrama.health.service.HealthProbeRunner;

/**
 * 聚合规则与 HTTP 状态映射的契约测试（Standards §13.3）。
 * 这两条规则是产品线统一方案里最容易被各仓「顺手改一下」的地方——改了就会让部署脚本的回滚判定
 * 与集中巡检平台的归一化同时失真，所以在本仓库用测试钉死。
 */
class HealthProbeRunnerTest {

    private static ComponentHealth component(String name, boolean critical, HealthStatus status) {
        return ComponentHealth.of(name, HealthComponentType.DATABASE, critical, status, "test",
                new LinkedHashMap<>());
    }

    @Test
    @DisplayName("全部 UP 时整体 UP")
    void aggregateAllUp() {
        List<ComponentHealth> components = List.of(
                component("db", true, HealthStatus.UP),
                component("disk_space", false, HealthStatus.UP));
        assertEquals(HealthStatus.UP, HealthProbeRunner.aggregate(components));
    }

    @Test
    @DisplayName("critical 组件 DOWN 时整体 DOWN")
    void aggregateCriticalDown() {
        List<ComponentHealth> components = List.of(
                component("db", true, HealthStatus.DOWN),
                component("disk_space", false, HealthStatus.UP));
        assertEquals(HealthStatus.DOWN, HealthProbeRunner.aggregate(components));
    }

    @Test
    @DisplayName("非 critical 组件 DOWN 只降级为 DEGRADED，不判死")
    void aggregateNonCriticalDownDegradesOnly() {
        List<ComponentHealth> components = List.of(
                component("db", true, HealthStatus.UP),
                component("disk_space", false, HealthStatus.DOWN));
        assertEquals(HealthStatus.DEGRADED, HealthProbeRunner.aggregate(components));
    }

    @Test
    @DisplayName("任一组件 DEGRADED 时整体 DEGRADED")
    void aggregateAnyDegraded() {
        List<ComponentHealth> components = List.of(
                component("db", true, HealthStatus.UP),
                component("jvm_memory", false, HealthStatus.DEGRADED));
        assertEquals(HealthStatus.DEGRADED, HealthProbeRunner.aggregate(components));
    }

    @Test
    @DisplayName("就绪判定忽略非 critical 组件，磁盘告警不该让发布失败")
    void aggregateCriticalIgnoresNonCritical() {
        List<ComponentHealth> components = List.of(
                component("db", true, HealthStatus.UP),
                component("disk_space", false, HealthStatus.DOWN));
        assertEquals(HealthStatus.UP, HealthProbeRunner.aggregateCritical(components));
    }

    @Test
    @DisplayName("就绪判定：critical 组件 DOWN 判 DOWN")
    void aggregateCriticalDownWins() {
        List<ComponentHealth> components = List.of(
                component("db", true, HealthStatus.DOWN),
                component("mybatis", true, HealthStatus.UP));
        assertEquals(HealthStatus.DOWN, HealthProbeRunner.aggregateCritical(components));
    }

    @Test
    @DisplayName("非 UP 组件名被收集，供一行 WARN 使用")
    void unhealthyNamesCollected() {
        List<ComponentHealth> components = List.of(
                component("db", true, HealthStatus.UP),
                component("mybatis", true, HealthStatus.DEGRADED),
                component("disk_space", false, HealthStatus.DOWN));
        assertEquals(List.of("mybatis", "disk_space"), HealthProbeRunner.unhealthyNames(components));
    }

    @Test
    @DisplayName("只有 DOWN 映射 503，DEGRADED 必须留在 200 以免劣化被误判成发布失败")
    void httpStatusMapping() {
        assertEquals(200, HealthHttpStatus.of(HealthStatus.UP).value());
        assertEquals(200, HealthHttpStatus.of(HealthStatus.DEGRADED).value());
        assertEquals(503, HealthHttpStatus.of(HealthStatus.DOWN).value());
    }
}
