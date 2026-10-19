package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;

class ShotVersionGuardTest {

    @Test
    void lockedAssetIsKeptAndMissingReferenceFails() {
        assertTrue(ShotVersionGuard.keepConfirmedAsset("LOCKED", "asset-1"));
        assertFalse(ShotVersionGuard.keepConfirmedAsset("FAILED", "asset-1"));
        assertFalse(ShotVersionGuard.keepConfirmedAsset("LOCKED", " "));
        assertThrows(BizException.class,
                () -> ShotVersionGuard.requireReferencedAsset("角色", "c1", false, false, false));
        assertThrows(BizException.class,
                () -> ShotVersionGuard.requireReferencedAsset("场景", "s1", true, false, false));
        assertDoesNotThrow(() -> ShotVersionGuard.requireReferencedAsset("道具", "p1", true, true, true));
        assertThrows(BizException.class,
                () -> ShotVersionGuard.rejectConsistencyLimitation("consistency limitation"));
    }
}
