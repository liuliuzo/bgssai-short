package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

import com.bgssai.shortai.model.ShortLookMaterial;
import com.bgssai.shortai.production.support.LookImage;

class LookKnowledgeMatchTest {

    @Test
    void nameInsideTheLookDescriptionScoresHighEnoughToAttach() {
        int score = LookKnowledgeService.matchScore("26岁女性，鹅蛋脸，黑色长直发", "沈知微", "鹅蛋脸 黑色长直发");
        assertTrue(score >= 3);
    }

    @Test
    void cardReferencePrefersFrontFaceOverLaterSidePhoto() {
        ShortLookMaterial side = material("SIDE", "side-key");
        ShortLookMaterial face = material("FRONT_FACE", "face-key");
        ShortLookMaterial chosen = LookKnowledgeService.pickCardReference(List.of(side, face));
        assertEquals("face-key", chosen.getStorageKey());
    }

    @Test
    void frontFaceOutranksAnotherAngleWithTheSameWords() {
        int face = LookKnowledgeService.rankScore(8, "FRONT_FACE");
        int side = LookKnowledgeService.rankScore(8, "SIDE");
        assertTrue(face > side);
    }

    @Test
    void twoSharedCharactersAreNotEnough() {
        assertTrue(LookKnowledgeService.matchScore("女性角色", "办公室", "落地窗") < 3);
    }

    private static ShortLookMaterial material(String angle, String key) {
        ShortLookMaterial row = new ShortLookMaterial();
        row.setViewAngle(angle);
        row.setStorageKey(key);
        return row;
    }

    @Test
    void retrievalKeepsTheFrontFaceWhenBothCaptionsMatch() {
        java.util.Map<String, Object> side = new java.util.LinkedHashMap<>();
        side.put("material_id", "side");
        side.put("material_name", "白纱花瓣");
        side.put("caption", "白色纱裙 侧面");
        side.put("view_angle", "SIDE");
        side.put("storage_key", "side-key");
        side.put("mime_type", "image/jpeg");
        java.util.Map<String, Object> face = new java.util.LinkedHashMap<>();
        face.put("material_id", "face");
        face.put("material_name", "白纱花瓣");
        face.put("caption", "白色纱裙 正脸");
        face.put("view_angle", "FRONT_FACE");
        face.put("storage_key", "face-key");
        face.put("mime_type", "image/jpeg");
        co.elastic.clients.elasticsearch.core.search.Hit<java.util.Map> sideHit =
                co.elastic.clients.elasticsearch.core.search.Hit.of(hit -> hit.index("short-test-look").id("side").source(side));
        co.elastic.clients.elasticsearch.core.search.Hit<java.util.Map> faceHit =
                co.elastic.clients.elasticsearch.core.search.Hit.of(hit -> hit.index("short-test-look").id("face").source(face));
        LookKnowledgeService.LookHit chosen = LookRagIndex.choose("白纱花瓣 白色纱裙", java.util.List.of(sideHit, faceHit));
        assertEquals("face-key", chosen.storageKey());
    }

    @Test
    void mp4BytesAreAcceptedAndKeepAPosterKey() {
        byte[] header = new byte[] {
                0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0,
                'i', 's', 'o', 'm', 'm', 'p', '4', '1'
        };
        String raw = java.util.Base64.getEncoder().encodeToString(header);
        LookImage.Decoded video = LookImage.decode(raw);
        assertEquals("video/mp4", video.mimeType());
        assertEquals("mp4", LookImage.extension(video.mimeType()));
        assertEquals("short/dev/u/look/lm-1.poster.jpg",
                LookKnowledgeService.posterKey("short/dev/u/look/lm-1.mp4"));
    }

    @Test
    void pngBytesAreAccepted() {
        LookImage.Decoded image = LookImage.decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        assertEquals("image/png", image.mimeType());
        assertTrue(image.bytes().length > 0);
    }

    @Test
    void sceneAndPropViewAnglesNormalizeRankAndPreferPrimaryView() {
        assertEquals("ESTABLISHING", LookKnowledgeService.normalizeViewForKind("SCENE", null, true));
        assertEquals("MAIN_VIEW", LookKnowledgeService.normalizeViewForKind("SCENE", "main_view", false));
        assertEquals("MAIN_PROP", LookKnowledgeService.normalizeViewForKind("PROP", null, true));
        assertEquals("全景主图", LookKnowledgeService.viewName("ESTABLISHING"));
        assertEquals("主视图", LookKnowledgeService.viewName("MAIN_PROP"));
        assertTrue(LookKnowledgeService.rankScore(8, "ESTABLISHING") > LookKnowledgeService.rankScore(8, "DETAIL_VIEW"));
        assertTrue(LookKnowledgeService.rankScore(8, "MAIN_PROP") > LookKnowledgeService.rankScore(8, "DETAIL_PROP"));

        ShortLookMaterial detail = material("DETAIL_VIEW", "scene-detail");
        detail.setMaterialKind("SCENE");
        ShortLookMaterial establishing = material("ESTABLISHING", "scene-main");
        establishing.setMaterialKind("SCENE");
        assertEquals("scene-main", LookKnowledgeService.pickCardReference(List.of(detail, establishing)).getStorageKey());
        assertEquals("ESTABLISHING", LookKnowledgeService.shownAngle(establishing));
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadFromAssetFilesIntoSceneEstablishingAndListShowsMixedKindsCleanly() {
        LookKnowledgeService service = new LookKnowledgeService();
        WorkAccessService access = org.mockito.Mockito.mock(WorkAccessService.class);
        com.bgssai.shortai.mapper.ShortLookLibraryMapper libraries = org.mockito.Mockito.mock(com.bgssai.shortai.mapper.ShortLookLibraryMapper.class);
        com.bgssai.shortai.mapper.ShortLookMaterialMapper materials = org.mockito.Mockito.mock(com.bgssai.shortai.mapper.ShortLookMaterialMapper.class);
        com.bgssai.shortai.mapper.ShortAssetMapper assets = org.mockito.Mockito.mock(com.bgssai.shortai.mapper.ShortAssetMapper.class);
        com.bgssai.shortai.storage.ObjectStorage storage = org.mockito.Mockito.mock(com.bgssai.shortai.storage.ObjectStorage.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "workAccessService", access);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "libraryMapper", libraries);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "materialMapper", materials);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "shortAssetMapper", assets);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "objectStorage", storage);

        org.mockito.Mockito.when(access.requireUser()).thenReturn("u-1");
        com.bgssai.shortai.model.ShortLookLibrary lib = new com.bgssai.shortai.model.ShortLookLibrary();
        lib.setId(1L);
        lib.setLibraryId("lk-1");
        lib.setUserId("u-1");
        lib.setLibraryName(LookKnowledgeService.DEFAULT_NAME);
        lib.setStatus("ACTIVE");
        lib.setMountPortrait(1);
        org.mockito.Mockito.when(libraries.selectByExample(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(lib));

        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        com.bgssai.shortai.model.ShortAsset asset = new com.bgssai.shortai.model.ShortAsset();
        asset.setAssetId("as-scene-1");
        asset.setUserId("u-1");
        asset.setAssetType("IMAGE");
        asset.setStorageKey("short/local/u-1/card/as-scene-1.png");
        org.mockito.Mockito.when(assets.selectOneByExample(org.mockito.ArgumentMatchers.any())).thenReturn(asset);
        org.mockito.Mockito.when(storage.get("short/local/u-1/card/as-scene-1.png")).thenReturn(png);

        com.bgssai.shortai.production.dto.LookDtos.LookMaterialReq req = new com.bgssai.shortai.production.dto.LookDtos.LookMaterialReq();
        req.setAssetId("as-scene-1");
        req.setMaterialKind("SCENE");
        req.setMaterialName("总裁办公室");
        req.setCaption("落地窗 冷色调");
        service.upload(req);

        org.mockito.ArgumentCaptor<ShortLookMaterial> saved = org.mockito.ArgumentCaptor.forClass(ShortLookMaterial.class);
        org.mockito.Mockito.verify(materials).insertSelective(saved.capture());
        assertEquals("SCENE", saved.getValue().getMaterialKind());
        assertEquals("ESTABLISHING", saved.getValue().getViewAngle());
        assertEquals("总裁办公室", saved.getValue().getMaterialName());
        assertEquals("as-scene-1", saved.getValue().getAssetId());

        ShortLookMaterial sameNameChar = material("FRONT_FACE", saved.getValue().getStorageKey());
        sameNameChar.setMaterialId("lm-char");
        sameNameChar.setMaterialKind("CHARACTER");
        sameNameChar.setMaterialName("总裁办公室");
        org.mockito.Mockito.when(materials.selectByExample(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(saved.getValue(), sameNameChar));

        java.util.Map<String, Object> listResp = (java.util.Map<String, Object>) service.list().getResult();
        List<java.util.Map<String, Object>> libs = (List<java.util.Map<String, Object>>) listResp.get("library_list");
        List<java.util.Map<String, Object>> items = (List<java.util.Map<String, Object>>) libs.get(0).get("material_list");
        assertEquals(2, items.size());
        assertEquals("全景主图", items.get(0).get("view_name"));
        assertEquals("正脸", items.get(1).get("view_name"));
    }
}
