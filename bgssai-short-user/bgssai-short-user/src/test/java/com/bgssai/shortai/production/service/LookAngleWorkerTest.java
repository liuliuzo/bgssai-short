package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortLookLibraryMapper;
import com.bgssai.shortai.mapper.ShortLookMaterialMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortLookLibrary;
import com.bgssai.shortai.model.ShortLookMaterial;
import com.bgssai.shortai.model.ShortLookMaterialExample;
import com.bgssai.shortai.storage.ObjectStorage;

class LookAngleWorkerTest {

    @Test
    void missingAnglesUseTheFacePhotoAndKeepTheOutfit() throws Exception {
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        AiProviderRegistry registry = mock(AiProviderRegistry.class);
        AiProvider provider = mock(AiProvider.class);
        ObjectStorage storage = mock(ObjectStorage.class);
        LookKnowledgeService looks = mock(LookKnowledgeService.class);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "objectStorage", storage);
        ReflectionTestUtils.setField(worker, "lookKnowledgeService", looks);
        when(registry.resolve(any())).thenReturn(provider);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 12, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] png = out.toByteArray();
        when(storage.get("face-key")).thenReturn(png);
        when(provider.execute(any())).thenReturn(AiProviderResult.binary(png, "image/png"));

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("library_id", "lk-1");
        req.put("material_name", "白衬衫");
        req.put("appearance", "齐刘海长马尾，白色短袖衬衫，深色短裤，黑色高跟鞋 白衬衫-正脸");
        req.put("reference_storage_key", "face-key");
        req.put("reference_mime", "image/png");
        req.put("angles", List.of("FRONT", "BACK", "SIDE"));
        ShortAiTask task = new ShortAiTask();
        task.setUserId("u-1");
        ReflectionTestUtils.invokeMethod(worker, "finishLookAngles", task, req, new AiModelConfig());

        ArgumentCaptor<AiProviderRequest> requests = ArgumentCaptor.forClass(AiProviderRequest.class);
        verify(provider, times(3)).execute(requests.capture());
        List<AiProviderRequest> sent = requests.getAllValues();
        assertTrue(sent.get(0).getPrompt().contains("前面全身"));
        assertTrue(sent.get(1).getPrompt().contains("背面全身"));
        assertTrue(sent.get(2).getPrompt().contains("侧面全身"));
        assertTrue(sent.stream().allMatch(item -> item.getPrompt().contains("深色短裤")
                && item.getPrompt().contains("不要换成别人")
                && item.param("image_base64") instanceof String encoded
                && !encoded.isBlank()));
        ArgumentCaptor<String> angles = ArgumentCaptor.forClass(String.class);
        verify(looks, times(3)).saveGenerated(eq("u-1"), eq("lk-1"), eq("白衬衫"), any(), angles.capture(),
                any(), eq("image/png"), anyInt(), anyInt(), isNull());
        assertEquals(List.of("FRONT", "BACK", "SIDE"), angles.getAllValues());
    }

    @Test
    void frontFaceRedoWorksWithoutMandatoryReferenceImage() throws Exception {
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        AiProviderRegistry registry = mock(AiProviderRegistry.class);
        AiProvider provider = mock(AiProvider.class);
        ObjectStorage storage = mock(ObjectStorage.class);
        LookKnowledgeService looks = mock(LookKnowledgeService.class);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "objectStorage", storage);
        ReflectionTestUtils.setField(worker, "lookKnowledgeService", looks);
        when(registry.resolve(any())).thenReturn(provider);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 12, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] png = out.toByteArray();
        when(provider.execute(any())).thenReturn(AiProviderResult.binary(png, "image/png"));

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("library_id", "lk-1");
        req.put("material_name", "红礼服");
        req.put("appearance", "黑色长卷发，正红色礼服长裙");
        req.put("angles", List.of("FRONT_FACE"));
        ShortAiTask task = new ShortAiTask();
        task.setUserId("u-1");
        ReflectionTestUtils.invokeMethod(worker, "finishLookAngles", task, req, new AiModelConfig());

        ArgumentCaptor<AiProviderRequest> requests = ArgumentCaptor.forClass(AiProviderRequest.class);
        verify(provider, times(1)).execute(requests.capture());
        AiProviderRequest sent = requests.getValue();
        assertTrue(sent.getPrompt().contains("正脸定妆"));
        assertTrue(sent.getPrompt().contains("正红色礼服长裙"));
        assertEquals(null, sent.param("image_base64"));
        verify(looks, times(1)).saveGenerated(eq("u-1"), eq("lk-1"), eq("红礼服"), any(), eq("FRONT_FACE"),
                any(), eq("image/png"), anyInt(), anyInt(), isNull());
    }

    @Test
    void batchWithoutInitialReferenceGeneratesFrontFaceFirstAndAnchorsRemainingAngles() throws Exception {
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        AiProviderRegistry registry = mock(AiProviderRegistry.class);
        AiProvider provider = mock(AiProvider.class);
        ObjectStorage storage = mock(ObjectStorage.class);
        LookKnowledgeService looks = mock(LookKnowledgeService.class);
        ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(worker, "objectStorage", storage);
        ReflectionTestUtils.setField(worker, "lookKnowledgeService", looks);
        when(registry.resolve(any())).thenReturn(provider);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 12, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] png = out.toByteArray();
        when(provider.execute(any())).thenReturn(AiProviderResult.binary(png, "image/png"));

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("library_id", "lk-1");
        req.put("material_name", "小明");
        req.put("appearance", "黑色西装，干练短发");
        req.put("card_id", "c-1");
        req.put("angles", List.of("FRONT_FACE", "FRONT", "BACK", "SIDE"));
        ShortAiTask task = new ShortAiTask();
        task.setUserId("u-1");
        ReflectionTestUtils.invokeMethod(worker, "finishLookAngles", task, req, new AiModelConfig());

        ArgumentCaptor<AiProviderRequest> requests = ArgumentCaptor.forClass(AiProviderRequest.class);
        verify(provider, times(4)).execute(requests.capture());
        List<AiProviderRequest> sent = requests.getAllValues();
        assertEquals(null, sent.get(0).param("image_base64"));
        assertTrue(sent.get(0).getPrompt().contains("正脸定妆"));
        for (int i = 1; i < 4; i++) {
            assertTrue(sent.get(i).param("image_base64") instanceof String encoded && !encoded.isBlank());
            assertEquals("subject", sent.get(i).param("image_reference"));
        }
        ArgumentCaptor<String> angles = ArgumentCaptor.forClass(String.class);
        verify(looks, times(4)).saveGenerated(eq("u-1"), eq("lk-1"), eq("小明"), any(), angles.capture(),
                any(), eq("image/png"), anyInt(), anyInt(), eq("c-1"));
        assertEquals(List.of("FRONT_FACE", "FRONT", "BACK", "SIDE"), angles.getAllValues());
    }

    @Test
    @SuppressWarnings("unchecked")
    void promoteMaterialSwapsOldPhotoBackWhilePreservingRedonePhoto() {
        LookKnowledgeService service = new LookKnowledgeService();
        WorkAccessService access = mock(WorkAccessService.class);
        com.bgssai.shortai.mapper.ShortLookLibraryMapper libraries = mock(com.bgssai.shortai.mapper.ShortLookLibraryMapper.class);
        com.bgssai.shortai.mapper.ShortLookMaterialMapper materials = mock(com.bgssai.shortai.mapper.ShortLookMaterialMapper.class);
        ObjectStorage storage = mock(ObjectStorage.class);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "libraryMapper", libraries);
        ReflectionTestUtils.setField(service, "materialMapper", materials);
        ReflectionTestUtils.setField(service, "objectStorage", storage);

        when(access.requireUser()).thenReturn("u-1");
        com.bgssai.shortai.model.ShortLookLibrary lib = new com.bgssai.shortai.model.ShortLookLibrary();
        lib.setId(1L);
        lib.setLibraryId("lk-1");
        lib.setUserId("u-1");
        lib.setStatus("ACTIVE");
        when(libraries.selectOneByExample(any())).thenReturn(lib);

        com.bgssai.shortai.model.ShortLookMaterial oldFace = new com.bgssai.shortai.model.ShortLookMaterial();
        oldFace.setId(10L);
        oldFace.setMaterialId("lm-old-face");
        oldFace.setLibraryId("lk-1");
        oldFace.setUserId("u-1");
        oldFace.setMaterialKind("CHARACTER");
        oldFace.setMaterialName("红礼服");
        oldFace.setCaption("红礼服 正脸");
        oldFace.setViewAngle("FRONT_FACE");
        oldFace.setStorageKey("short/dev/u-1/look/lm-old-face.png");
        oldFace.setMimeType("image/png");
        oldFace.setWidth(720);
        oldFace.setHeight(1080);
        oldFace.setFileSizeByte(102400);
        oldFace.setDeletedFlag(0);

        when(materials.selectOneByExample(any())).thenReturn(oldFace);
        when(materials.selectByExample(any())).thenReturn(List.of(oldFace));

        com.bgssai.shortai.production.dto.LookDtos.LookMaterialReq promoteReq = new com.bgssai.shortai.production.dto.LookDtos.LookMaterialReq();
        promoteReq.setMaterialId("lm-old-face");
        com.bgssai.shortai.common.ApiResponse resp = service.promoteMaterial(promoteReq);
        assertTrue(resp.isSuccess());
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals("FRONT_FACE", result.get("view_angle"));
        assertEquals("正脸", result.get("view_name"));

        ArgumentCaptor<com.bgssai.shortai.model.ShortLookMaterial> inserted =
                ArgumentCaptor.forClass(com.bgssai.shortai.model.ShortLookMaterial.class);
        verify(materials).insertSelective(inserted.capture());
        assertEquals("short/dev/u-1/look/lm-old-face.png", inserted.getValue().getStorageKey());
        assertEquals("FRONT_FACE", inserted.getValue().getViewAngle());
        verify(materials).updateByExampleSelective(any(), any());
    }

    @Test
    void saveGeneratedPersistsFrontFaceAssetLinkThroughRealMybatisMapper() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
                CREATE TABLE short_look_library (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    library_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    library_name VARCHAR(120) NOT NULL,
                    description VARCHAR(500),
                    status VARCHAR(16) NOT NULL,
                    mount_portrait TINYINT NOT NULL DEFAULT 1,
                    item_count INT NOT NULL DEFAULT 0,
                    usage_bytes BIGINT NOT NULL DEFAULT 0,
                    deleted_flag TINYINT NOT NULL DEFAULT 0,
                    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");
        jdbc.execute("""
                CREATE TABLE short_look_material (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    material_id VARCHAR(64) NOT NULL,
                    library_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    material_name VARCHAR(120) NOT NULL,
                    caption VARCHAR(500),
                    material_kind VARCHAR(16) NOT NULL,
                    view_angle VARCHAR(16),
                    card_id VARCHAR(64),
                    asset_id VARCHAR(64),
                    mime_type VARCHAR(64) NOT NULL,
                    storage_key VARCHAR(512) NOT NULL,
                    width INT,
                    height INT,
                    file_size_byte INT NOT NULL DEFAULT 0,
                    deleted_flag TINYINT NOT NULL DEFAULT 0,
                    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");
        jdbc.execute("""
                CREATE TABLE short_character (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    character_id VARCHAR(64) NOT NULL,
                    work_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    character_name VARCHAR(64) NOT NULL,
                    character_role VARCHAR(32),
                    card_status VARCHAR(32) NOT NULL,
                    appearance_prompt VARCHAR(1024),
                    consistency_seed BIGINT,
                    ref_image_asset_id VARCHAR(64),
                    voice_id VARCHAR(64),
                    voice_speed VARCHAR(16),
                    deleted_flag TINYINT NOT NULL DEFAULT 0,
                    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");
        jdbc.execute("""
                CREATE TABLE short_asset (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    asset_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    work_id VARCHAR(64),
                    episode_id VARCHAR(64),
                    shot_id VARCHAR(64),
                    asset_type VARCHAR(16) NOT NULL,
                    mime_type VARCHAR(128),
                    storage_key LONGTEXT,
                    file_url VARCHAR(1024),
                    file_size_byte INT,
                    duration_sec INT,
                    width INT,
                    height INT,
                    source_task_id VARCHAR(64),
                    selected_flag TINYINT NOT NULL DEFAULT 0,
                    draft_flag TINYINT NOT NULL DEFAULT 0,
                    deleted_flag TINYINT NOT NULL DEFAULT 0,
                    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setMapperLocations(
                new ClassPathResource("mybatis/mapper/ShortLookLibraryMapper.xml"),
                new ClassPathResource("mybatis/mapper/ShortLookMaterialMapper.xml"),
                new ClassPathResource("mybatis/mapper/ShortCharacterMapper.xml"),
                new ClassPathResource("mybatis/mapper/ShortAssetMapper.xml"));
        SqlSessionTemplate session = new SqlSessionTemplate(factory.getObject());
        ShortLookLibraryMapper libraryMapper = session.getMapper(ShortLookLibraryMapper.class);
        ShortLookMaterialMapper materialMapper = session.getMapper(ShortLookMaterialMapper.class);
        ShortCharacterMapper characterMapper = session.getMapper(ShortCharacterMapper.class);
        ShortAssetMapper assetMapper = session.getMapper(ShortAssetMapper.class);

        ShortLookLibrary lib = new ShortLookLibrary();
        lib.setLibraryId("lk-1");
        lib.setUserId("u-1");
        lib.setLibraryName(LookKnowledgeService.DEFAULT_NAME);
        lib.setStatus("ACTIVE");
        lib.setMountPortrait(1);
        lib.setItemCount(0);
        lib.setUsageBytes(0L);
        lib.setDeletedFlag(0);
        lib.setCreatedTime(new Date());
        libraryMapper.insertSelective(lib);

        ShortCharacter character = new ShortCharacter();
        character.setCharacterId("c-1");
        character.setWorkId("w-1");
        character.setUserId("u-1");
        character.setCharacterName("小明");
        character.setCardStatus("DRAFT");
        character.setDeletedFlag(0);
        character.setCreatedTime(new Date());
        characterMapper.insertSelective(character);

        LookKnowledgeService service = new LookKnowledgeService();
        ObjectStorage storage = mock(ObjectStorage.class);
        ReflectionTestUtils.setField(service, "libraryMapper", libraryMapper);
        ReflectionTestUtils.setField(service, "materialMapper", materialMapper);
        ReflectionTestUtils.setField(service, "shortCharacterMapper", characterMapper);
        ReflectionTestUtils.setField(service, "shortAssetMapper", assetMapper);
        ReflectionTestUtils.setField(service, "objectStorage", storage);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 12, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] png = out.toByteArray();

        for (String angle : List.of("FRONT_FACE", "FRONT", "BACK", "SIDE")) {
            service.saveGenerated("u-1", "lk-1", "小明", "小明 " + angle, angle, png, "image/png", 8, 12, "c-1");
        }

        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andCharacterIdEqualTo("c-1").andDeletedFlagEqualTo(0);
        ShortCharacter savedChar = characterMapper.selectOneByExample(cx);
        assertNotNull(savedChar);
        assertEquals("LOCKED", savedChar.getCardStatus());
        assertNotNull(savedChar.getRefImageAssetId());

        ShortLookMaterialExample mx = new ShortLookMaterialExample();
        mx.createCriteria().andCardIdEqualTo("c-1").andDeletedFlagEqualTo(0);
        mx.setOrderByClause("id asc");
        List<ShortLookMaterial> stored = materialMapper.selectByExample(mx);
        assertEquals(4, stored.size());
        ShortLookMaterial frontFace = stored.stream()
                .filter(row -> "FRONT_FACE".equals(row.getViewAngle()))
                .findFirst()
                .orElseThrow();
        assertEquals(savedChar.getRefImageAssetId(), frontFace.getAssetId());

        // Verify selective update with only assetId set never emits an empty SET clause.
        ShortLookMaterial assetOnlyPatch = new ShortLookMaterial();
        assetOnlyPatch.setAssetId("a-relinked");
        ShortLookMaterialExample patchEx = new ShortLookMaterialExample();
        patchEx.createCriteria().andIdEqualTo(frontFace.getId());
        assertEquals(1, materialMapper.updateByExampleSelective(assetOnlyPatch, patchEx));
        ShortLookMaterialExample checkEx = new ShortLookMaterialExample();
        checkEx.createCriteria().andIdEqualTo(frontFace.getId());
        assertEquals("a-relinked", materialMapper.selectOneByExample(checkEx).getAssetId());
    }
}
