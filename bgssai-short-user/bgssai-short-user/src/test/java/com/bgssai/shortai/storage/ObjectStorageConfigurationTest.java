package com.bgssai.shortai.storage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;

class ObjectStorageConfigurationTest {

    @TempDir
    Path tempDir;

    @Test
    void nonLocalProfileWithoutObsFailsFast() {
        ObjectStorageConfiguration config = new ObjectStorageConfiguration();
        ObsProperties props = new ObsProperties();
        props.setLocalRoot(tempDir.toString());
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config.objectStorage(props, env));
        assertTrue(ex.getMessage().contains("OBS") || ex.getMessage().contains("local"));
        assertTrue(ex.getMessage().contains("SHORT-01") || ex.getMessage().contains("LocalFile"));
    }

    @Test
    void localProfileAllowsLocalFileWhenObsMissing() {
        ObjectStorageConfiguration config = new ObjectStorageConfiguration();
        ObsProperties props = new ObsProperties();
        props.setLocalRoot(tempDir.toString());
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        ObjectStorage storage = config.objectStorage(props, env);
        assertInstanceOf(LocalFileObjectStorage.class, storage);
    }

    @Test
    void blankActiveProfilesTreatedAsNonLocal() {
        ObjectStorageConfiguration config = new ObjectStorageConfiguration();
        ObsProperties props = new ObsProperties();
        props.setLocalRoot(tempDir.toString());
        StandardEnvironment env = new StandardEnvironment();
        assertThrows(IllegalStateException.class, () -> config.objectStorage(props, env));
    }
}
