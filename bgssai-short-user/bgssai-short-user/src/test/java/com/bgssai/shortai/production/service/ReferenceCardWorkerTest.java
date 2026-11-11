package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import com.bgssai.shortai.aiprovider.*;
import com.bgssai.shortai.mapper.*;
import com.bgssai.shortai.model.*;
import com.bgssai.shortai.storage.ObjectStorage;

class ReferenceCardWorkerTest {
    @ParameterizedTest @ValueSource(booleans={false,true})
    void completionPreservesLockConditionAndUsesDescriptionFirst(boolean sceneTask) throws Exception {
        for (boolean otherActive : new boolean[]{false,true}) {
            ShortAiTaskWorker worker = new ShortAiTaskWorker();
            ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
            ShortCharacterMapper characters = mock(ShortCharacterMapper.class);
            ShortSceneCardMapper scenes = mock(ShortSceneCardMapper.class);
            AiProviderRegistry registry = mock(AiProviderRegistry.class);
            AiProvider provider = mock(AiProvider.class);
            ShortAssetMapper assets = mock(ShortAssetMapper.class);
            ReflectionTestUtils.setField(worker,"shortAiTaskMapper",tasks);
            ReflectionTestUtils.setField(worker,"shortCharacterMapper",characters);
            ReflectionTestUtils.setField(worker,"shortSceneCardMapper",scenes);
            ReflectionTestUtils.setField(worker,"shortAssetMapper",assets);
            ReflectionTestUtils.setField(worker,"aiProviderRegistry",registry);
            ReflectionTestUtils.setField(worker,"objectStorage",mock(ObjectStorage.class));
            ReflectionTestUtils.setField(worker,"environment",new MockEnvironment());
            when(tasks.countByExample(any())).thenReturn(otherActive ? 1L : 0L);
            when(registry.resolve(any())).thenReturn(provider);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(2,3,BufferedImage.TYPE_INT_RGB),"png",out);
            when(provider.execute(any())).thenReturn(AiProviderResult.binary(out.toByteArray(),"image/png"));
            ShortAiTask task = new ShortAiTask(); task.setTaskId("t-1"); task.setTargetId("ref-1");
            String description = "A single elderly shopkeeper, grey coat, close portrait";
            ShortCharacter c = new ShortCharacter(); c.setId(1L); c.setCharacterId("ref-1"); c.setCharacterName("老陈");
            c.setUserId("u-1"); c.setWorkId("w-1"); c.setAppearancePrompt(description);
            ShortSceneCard sc = new ShortSceneCard(); sc.setId(1L); sc.setSceneCardId("ref-1"); sc.setCardName("雨夜便利店");
            sc.setUserId("u-1"); sc.setWorkId("w-1"); sc.setAppearancePrompt(description);
            when(characters.selectOneByExample(any())).thenReturn(c); when(scenes.selectOneByExample(any())).thenReturn(sc);
            ReflectionTestUtils.invokeMethod(worker,sceneTask ? "finishScenePortrait" : "finishPortrait",task,
                    Map.of("candidate_count",2),new AiModelConfig());
            ArgumentCaptor<AiProviderRequest> requests = ArgumentCaptor.forClass(AiProviderRequest.class);
            verify(provider,times(2)).execute(requests.capture());
            assertTrue(requests.getAllValues().stream().allMatch(r -> description.equals(r.getPrompt())));
            String expected = otherActive ? "GENERATING" : "DRAFT";
            if (sceneTask) {
                ArgumentCaptor<ShortSceneCard> patch = ArgumentCaptor.forClass(ShortSceneCard.class);
                ArgumentCaptor<ShortSceneCardExample> where = ArgumentCaptor.forClass(ShortSceneCardExample.class);
                verify(scenes).updateByExampleSelective(patch.capture(),where.capture());
                assertEquals(expected,patch.getValue().getCardStatus());
                assertTrue(where.getValue().getOredCriteria().get(0).getAllCriteria().stream()
                        .anyMatch(v -> "card_status <>".equals(v.getCondition()) && "LOCKED".equals(v.getValue())));
            } else {
                ArgumentCaptor<ShortCharacter> patch = ArgumentCaptor.forClass(ShortCharacter.class);
                ArgumentCaptor<ShortCharacterExample> where = ArgumentCaptor.forClass(ShortCharacterExample.class);
                verify(characters).updateByExampleSelective(patch.capture(),where.capture());
                assertEquals(expected,patch.getValue().getCardStatus());
                assertTrue(where.getValue().getOredCriteria().get(0).getAllCriteria().stream()
                        .anyMatch(v -> "card_status <>".equals(v.getCondition()) && "LOCKED".equals(v.getValue())));
            }
            ArgumentCaptor<ShortAiTaskExample> criteria = ArgumentCaptor.forClass(ShortAiTaskExample.class);
            verify(tasks).countByExample(criteria.capture());
            assertTrue(criteria.getValue().getOredCriteria().get(0).getAllCriteria().stream()
                    .anyMatch(v -> "task_id <>".equals(v.getCondition()) && "t-1".equals(v.getValue())));
            verify(assets,times(2)).insertSelective(any());
        }
    }
}
