package com.bgssai.shortdrama.health.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortdrama.common.ApiResponse;
import com.bgssai.shortdrama.health.HealthHttpStatus;
import com.bgssai.shortdrama.health.dto.HealthReport;
import com.bgssai.shortdrama.health.service.QueryHealthReportService;

/**
 * GET /bgssai/health —— 公开全量健康报告（Standards §13.3 / §13.4）；不带 {@code jwtToken}。
 * 聚合状态为 DOWN 时 HTTP 503，其余 200。
 */
@RestController
public class QueryHealthReportController {

    @Autowired
    private QueryHealthReportService queryHealthReportService;

    @GetMapping("/bgssai/health")
    public ResponseEntity<ApiResponse> handle() {
        HealthReport report = queryHealthReportService.report();
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(report);
        return ResponseEntity.status(HealthHttpStatus.of(report.getStatus())).body(response);
    }
}
