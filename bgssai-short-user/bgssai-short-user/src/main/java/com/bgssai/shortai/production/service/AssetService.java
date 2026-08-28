package com.bgssai.shortai.production.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ObjectStorage objectStorage;

    /**
     * 把 asset_id 解析成浏览器可直接用的地址：内联 SVG 种子返回 data URL，其余走 OBS 预签名。
     * <p>
     * 列表接口以前返回的是 {@code /bgssai/user/assets/{id}}，那是个要带 Jwttoken 请求头、且返回
     * JSON 的接口；前端把它塞进 &lt;img src&gt; 后浏览器既不带头也拿不到图片字节，画面必然是裂的。
     * 预签名是本地 HMAC 计算，不发网络请求，可以安全地在列表里逐条调用。
     */
    public String resolveUrl(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ex);
        if (asset == null) {
            return null;
        }
        String key = asset.getStorageKey();
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.startsWith("<svg")) {
            return "data:image/svg+xml;base64,"
                    + Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
        }
        try {
            return objectStorage.presignGet(key, PRESIGN_SECONDS).downloadUrl();
        } catch (RuntimeException ex2) {
            // 签名失败不该让整张列表 500：这一条没图，其它照常返回。
            log.warn("presign failed assetId={}", assetId, ex2);
            return null;
        }
    }

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
