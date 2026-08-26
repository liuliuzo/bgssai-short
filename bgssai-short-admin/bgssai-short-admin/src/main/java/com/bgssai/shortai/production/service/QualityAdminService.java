package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortReferenceTemplateMapper;
import com.bgssai.shortai.model.ShortReferenceTemplate;
import com.bgssai.shortai.model.ShortReferenceTemplateExample;
import com.bgssai.shortai.production.dto.QualityAdminDtos.TemplateReq;
import com.bgssai.shortai.production.dto.QualityAdminDtos.ThresholdSaveReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class QualityAdminService {

    private static final Map<String, Map<String, Object>> THRESHOLDS = new ConcurrentHashMap<>();

    @Autowired
    private ShortReferenceTemplateMapper shortReferenceTemplateMapper;

    public ApiResponse listTemplates() {
        requireAdmin();
        ShortReferenceTemplateExample ex = new ShortReferenceTemplateExample();
        ex.createCriteria().andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortReferenceTemplate t : shortReferenceTemplateMapper.selectByExample(ex)) {
            list.add(view(t));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("template_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse create(TemplateReq req) {
        requireAdmin();
        if (req == null || req.getTemplateName() == null || req.getTemplateName().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "template_name required");
        }
        ShortReferenceTemplate row = new ShortReferenceTemplate();
        row.setTemplateId(ShortIds.id("tpl-"));
        apply(row, req);
        row.setEnabledFlag(req.getEnabledFlag() == null ? 1 : req.getEnabledFlag());
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        row.setUpdatedTime(new Date());
        shortReferenceTemplateMapper.insertSelective(row);
        return Responses.ok(view(row));
    }

    @Transactional
    public ApiResponse update(TemplateReq req) {
        requireAdmin();
        ShortReferenceTemplate existing = requireTemplate(req == null ? null : req.getTemplateId());
        ShortReferenceTemplate patch = new ShortReferenceTemplate();
        apply(patch, req);
        patch.setUpdatedTime(new Date());
        ShortReferenceTemplateExample up = new ShortReferenceTemplateExample();
        up.createCriteria().andIdEqualTo(existing.getId());
        shortReferenceTemplateMapper.updateByExampleSelective(patch, up);
        return Responses.ok(view(requireTemplate(existing.getTemplateId())));
    }

    @Transactional
    public ApiResponse toggleEnabled(TemplateReq req) {
        requireAdmin();
        ShortReferenceTemplate existing = requireTemplate(req == null ? null : req.getTemplateId());
        int next = Integer.valueOf(1).equals(existing.getEnabledFlag()) ? 0 : 1;
        if (req.getEnabledFlag() != null) {
            next = req.getEnabledFlag();
        }
        ShortReferenceTemplate patch = new ShortReferenceTemplate();
        patch.setEnabledFlag(next);
        patch.setUpdatedTime(new Date());
        ShortReferenceTemplateExample up = new ShortReferenceTemplateExample();
        up.createCriteria().andIdEqualTo(existing.getId());
        shortReferenceTemplateMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("template_id", existing.getTemplateId());
        result.put("enabled_flag", next);
        return Responses.ok(result);
    }

    public ApiResponse thresholdSave(ThresholdSaveReq req) {
        requireAdmin();
        String genre = req == null || req.getGenre() == null ? "DEFAULT" : req.getGenre();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("genre", genre);
        row.put("default_threshold_score",
                req.getDefaultThresholdScore() == null ? 75 : req.getDefaultThresholdScore());
        row.put("first_episode_threshold_score",
                req.getFirstEpisodeThresholdScore() == null ? 85 : req.getFirstEpisodeThresholdScore());
        THRESHOLDS.put(genre, row);
        return Responses.ok(row);
    }

    private void apply(ShortReferenceTemplate row, TemplateReq req) {
        if (req.getTemplateName() != null) {
            row.setTemplateName(req.getTemplateName());
        }
        if (req.getGenre() != null) {
            row.setGenre(req.getGenre());
        }
        if (req.getHookTypeList() != null) {
            row.setHookTypeJson(ShortJson.toJson(req.getHookTypeList()));
        }
        if (req.getBeatSheetJson() != null) {
            row.setBeatSheetJson(ShortJson.toJson(req.getBeatSheetJson()));
        }
        if (req.getTurningPointJson() != null) {
            row.setTurningPointJson(ShortJson.toJson(req.getTurningPointJson()));
        }
        if (req.getTabooList() != null) {
            row.setTabooListJson(ShortJson.toJson(req.getTabooList()));
        }
        if (req.getEnabledFlag() != null) {
            row.setEnabledFlag(req.getEnabledFlag());
        }
    }

    private ShortReferenceTemplate requireTemplate(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "template_id required");
        }
        ShortReferenceTemplateExample ex = new ShortReferenceTemplateExample();
        ex.createCriteria().andTemplateIdEqualTo(templateId).andDeletedFlagEqualTo(0);
        ShortReferenceTemplate row = shortReferenceTemplateMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "template not found");
        }
        return row;
    }

    private Map<String, Object> view(ShortReferenceTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("template_id", t.getTemplateId());
        m.put("template_name", t.getTemplateName());
        m.put("genre", t.getGenre());
        m.put("hook_type_list", ShortJson.list(t.getHookTypeJson()));
        m.put("beat_sheet_json", ShortJson.map(t.getBeatSheetJson()).isEmpty()
                ? ShortJson.list(t.getBeatSheetJson()) : ShortJson.map(t.getBeatSheetJson()));
        m.put("turning_point_json", ShortJson.map(t.getTurningPointJson()));
        m.put("taboo_list", ShortJson.list(t.getTabooListJson()));
        m.put("enabled_flag", t.getEnabledFlag());
        return m;
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("admin required");
        }
    }
}
