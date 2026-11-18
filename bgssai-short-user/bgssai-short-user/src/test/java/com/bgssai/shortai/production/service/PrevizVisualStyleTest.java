package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bgssai.shortai.mapper.ShortPromptVersionMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortShotPrevizMapper;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.FeatureDtos.PrevizSaveReq;

@ExtendWith(MockitoExtension.class)
class PrevizVisualStyleTest {
    @Mock WorkAccessService access;
    @Mock ShortShotMapper shots;
    @Mock ShortShotPrevizMapper previz;
    @Mock ShortPromptVersionMapper versions;
    @InjectMocks PrevizService service;

    @ParameterizedTest
    @ValueSource(strings = {"谁：她。做什么：推开门。 【画面风格：黑白胶片】", "谁：她。做什么：推开门。", ""})
    void savingTheNewDirectionReplacesOrClearsThePreviousStyle(String newText) {
        ShortShot shot = new ShortShot();
        shot.setId(1L);
        shot.setShotId("s-1");
        shot.setUserId("u-1");
        shot.setWorkId("w-1");
        shot.setShotDesc("她关上门");
        shot.setPrevizText("【画面风格：三维动画】");
        shot.setImagePrompt("【画面风格：三维动画】 她关上门");
        boolean selected = newText.contains("【画面风格：黑白胶片】");
        if (!selected) {
            ShortWork work = new ShortWork();
            work.setVisualStyle("ANIME");
            when(access.requireWork("w-1")).thenReturn(work);
        }
        when(access.requireUser()).thenReturn("u-1");
        when(shots.selectOneByExample(any())).thenReturn(shot);
        PrevizSaveReq req = new PrevizSaveReq();
        req.setShotId("s-1");
        req.setPrevizText(newText);
        if (newText.isEmpty()) req.setImagePrompt(shot.getImagePrompt());

        Map<?, ?> result = (Map<?, ?>) service.save(req).getResult();
        String prompt = (String) result.get("image_prompt");
        assertTrue(prompt.contains(newText.isEmpty() ? "她关上门" : "推开门"));
        assertFalse(prompt.contains("三维动画"));
        assertFalse(prompt.contains("photorealistic"));
        assertEquals(!selected, prompt.contains("动漫风格"));
        assertEquals(selected, prompt.startsWith("【画面风格：黑白胶片】"));
        verify(versions).insertSelective(argThat(version -> prompt.equals(version.getImagePrompt())));
        verify(previz).insertSelective(argThat(row -> req.getPrevizText().equals(row.getPrevizText())));
    }
}
