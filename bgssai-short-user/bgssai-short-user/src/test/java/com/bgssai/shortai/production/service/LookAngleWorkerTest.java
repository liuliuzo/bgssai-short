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
}
