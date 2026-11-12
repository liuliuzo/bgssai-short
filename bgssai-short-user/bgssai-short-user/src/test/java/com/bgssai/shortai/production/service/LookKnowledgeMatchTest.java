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
}
