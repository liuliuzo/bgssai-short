package com.bgssai.shortai.production.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.storage.ObjectStorage;

@Service
public class AssetService {

    private static final int PRESIGN_SECONDS = 1800;

    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ObjectStorage objectStorage;

    public ApiResponse getDownload(String assetId) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new ForbiddenException("login required");
        }
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ex);
        if (asset == null) {
            throw new ForbiddenException("asset not found");
        }
        if (!userId.equals(asset.getUserId())) {
            throw new ForbiddenException("asset not found");
        }
        String key = asset.getStorageKey();
        Map<String, Object> result = new LinkedHashMap<>();
        if (key != null && key.startsWith("<svg")) {
            String dataUrl = "data:image/svg+xml;base64,"
                    + Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
            Instant exp = Instant.now().plusSeconds(PRESIGN_SECONDS);
            result.put("download_url", dataUrl);
            result.put("expire_at", DateTimeFormatter.ISO_INSTANT.format(exp));
            return Responses.ok(result);
        }
        if (key == null || key.isBlank()) {
            throw new ForbiddenException("asset not found");
        }
        ObjectStorage.PresignResult signed = objectStorage.presignGet(key, PRESIGN_SECONDS);
        result.put("download_url", signed.downloadUrl());
        result.put("expire_at", DateTimeFormatter.ISO_INSTANT.format(signed.expireAt()));
        return Responses.ok(result);
    }
}
