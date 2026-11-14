package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
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
}
