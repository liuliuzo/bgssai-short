package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.production.support.LookImage;

class LookKnowledgeMatchTest {

    @Test
    void nameInsideTheLookDescriptionScoresHighEnoughToAttach() {
        int score = LookKnowledgeService.matchScore("26岁女性，鹅蛋脸，黑色长直发", "沈知微", "鹅蛋脸 黑色长直发");
        assertTrue(score >= 3);
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

    @Test
    void pngBytesAreAccepted() {
        LookImage.Decoded image = LookImage.decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        assertEquals("image/png", image.mimeType());
        assertTrue(image.bytes().length > 0);
    }
}
