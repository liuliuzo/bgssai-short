package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.AssetService;

@RestController
public class GetAssetController {

    @Autowired
    private AssetService assetService;

    @NeedAop
    @GetMapping("/bgssai/user/assets/{asset_id}")
    public ApiResponse handle(@PathVariable("asset_id") String assetId) {
        return assetService.getDownload(assetId);
    }
}
