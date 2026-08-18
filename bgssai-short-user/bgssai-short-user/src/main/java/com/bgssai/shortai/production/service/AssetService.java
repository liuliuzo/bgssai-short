package com.bgssai.shortai.production.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;

@Service
public class AssetService {

    @Autowired
    private ShortAssetMapper shortAssetMapper;

    public ResponseEntity<byte[]> get(String assetId) {
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ex);
        if (asset == null || asset.getStorageKey() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = asset.getStorageKey().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String contentType = "FILM".equals(asset.getAssetType()) || (asset.getStorageKey() != null
                && asset.getStorageKey().startsWith("{"))
                ? MediaType.APPLICATION_JSON_VALUE
                : "image/svg+xml";
        if (asset.getStorageKey().startsWith("<svg")) {
            contentType = "image/svg+xml";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
