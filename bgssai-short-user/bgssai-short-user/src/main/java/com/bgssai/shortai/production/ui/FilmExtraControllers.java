package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmExportReq;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmRebakeReq;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmSettingsSaveReq;
import com.bgssai.shortai.production.service.FilmService;

@RestController
public class FilmExtraControllers {

    @Autowired
    private FilmService service;

    @NeedAop
    @PostMapping("/bgssai/user/films/rebake-subtitle")
    public ApiResponse rebake(@RequestBody FilmRebakeReq req) {
        return service.rebakeSubtitle(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/films/settings-save")
    public ApiResponse settings(@RequestBody FilmSettingsSaveReq req) {
        return service.settingsSave(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/films/export")
    public ApiResponse export(@RequestBody FilmExportReq req) {
        return service.export(req);
    }
}
