package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityForcePassReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityGateRunReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityIssueHandleReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityThresholdAdjustReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityVariantGenerateReq;
import com.bgssai.shortai.production.dto.FeatureDtos.QualityVariantSelectReq;
import com.bgssai.shortai.production.service.QualityService;

@RestController
public class QualityControllers {

    @Autowired
    private QualityService service;

    @NeedAop
    @PostMapping("/bgssai/user/quality/gate-run")
    public ApiResponse gateRun(@RequestBody QualityGateRunReq req) {
        return service.gateRun(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/quality/force-pass")
    public ApiResponse forcePass(@RequestBody QualityForcePassReq req) {
        return service.forcePass(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/quality/issue-handle")
    public ApiResponse issueHandle(@RequestBody QualityIssueHandleReq req) {
        return service.issueHandle(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/quality/threshold-adjust")
    public ApiResponse thresholdAdjust(@RequestBody QualityThresholdAdjustReq req) {
        return service.thresholdAdjust(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/quality/variants-generate")
    public ApiResponse variantsGenerate(@RequestBody QualityVariantGenerateReq req) {
        return service.variantsGenerate(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/quality/variant-select")
    public ApiResponse variantSelect(@RequestBody QualityVariantSelectReq req) {
        return service.variantSelect(req);
    }

    @NeedAop
    @GetMapping("/bgssai/user/quality/gate-result")
    public ApiResponse gateResult(@RequestParam(value = "gate_code", required = false) String gateCode,
            @RequestParam("episode_id") String episodeId) {
        return service.gateResult(gateCode, episodeId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/quality/threshold-logs")
    public ApiResponse thresholdLogs(@RequestParam("work_id") String workId) {
        return service.thresholdLogs(workId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/quality/variants")
    public ApiResponse variants(@RequestParam(value = "variant_node", required = false) String variantNode,
            @RequestParam(value = "target_id", required = false) String targetId) {
        return service.variants(variantNode, targetId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/quality/profile")
    public ApiResponse profile(@RequestParam("work_id") String workId) {
        return service.profile(workId);
    }
}
