package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CardAssetSelectionTest {

    @Test
    void lockedPhotoWinsOverAnOlderSelectedCandidate() {
        assertEquals(1, CardAssetSelection.shownFlag("photo", "photo", 0));
        assertEquals(0, CardAssetSelection.shownFlag("woodcut", "photo", 1));
    }

    @Test
    void withoutALockTheStoredChoiceRemains() {
        assertEquals(1, CardAssetSelection.shownFlag("woodcut", null, 1));
        assertEquals(0, CardAssetSelection.shownFlag("other", "  ", 0));
    }
}
