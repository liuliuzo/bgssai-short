package com.bgssai.shortai.useraimodel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mockito.Answers;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.UserAiModelKeyMapper;
import com.bgssai.shortai.mapper.UserAiSettingMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.UserAiModelKey;
import com.bgssai.shortai.model.UserAiModelKeyExample;
import com.bgssai.shortai.model.UserAiSetting;
import com.bgssai.shortai.model.UserAiSettingExample;
import com.bgssai.shortai.useraimodel.dto.UpdateUserAiKeyModeRequest;

/**
 * There is no global "only my own keys" mode anymore: an enabled own-key model can be
 * selected explicitly, and anything not enabled or not selected falls back to the
 * BGSSAI built-in models. A legacy stored exclusive flag must not change that.
 */
class UserAiModelKeyFallbackTest {

    private final AtomicReference<List<UserAiModelKey>> enabledRows = new AtomicReference<>(List.of());
    private final AtomicReference<UserAiSetting> storedSetting = new AtomicReference<>();
    private UserAiModelKeyService service;

    @BeforeEach
    void setUp() {
        UserAiSetting legacyExclusive = new UserAiSetting();
        legacyExclusive.setId(7L);
        legacyExclusive.setUserId("u1");
        legacyExclusive.setOnlyUserKey(1);
        storedSetting.set(legacyExclusive);

        UserAiModelKeyMapper keyMapper = mock(UserAiModelKeyMapper.class);
        when(keyMapper.selectByExample(any(UserAiModelKeyExample.class))).thenAnswer(inv -> enabledRows.get());

        UserAiSettingMapper settingMapper = mock(UserAiSettingMapper.class);
        when(settingMapper.selectOneByExample(any(UserAiSettingExample.class)))
                .thenAnswer(inv -> storedSetting.get());
        when(settingMapper.updateByPrimaryKeySelective(any(UserAiSetting.class))).thenAnswer(inv -> {
            UserAiSetting patch = inv.getArgument(0);
            if (patch.getOnlyUserKey() != null && storedSetting.get() != null) {
                storedSetting.get().setOnlyUserKey(patch.getOnlyUserKey());
            }
            return 1;
        });

        // Real methods without running field initializers: the constructor-side HttpClient
        // for connectivity probes is unrelated to model resolution and must not be built here.
        service = mock(UserAiModelKeyService.class, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
        injectByType(service, UserAiModelKeyMapper.class, keyMapper);
        injectByType(service, UserAiSettingMapper.class, settingMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void noEnabledOwnModelFallsBackToPlatformEvenWithLegacyExclusiveFlag() {
        enabledRows.set(List.of());

        assertNull(service.resolveUserModel("u1", "TEXT", null));
        assertNull(service.resolveUserModel("u1", "TEXT", "bgssai-chat"));
    }

    @Test
    void enabledOwnModelStillResolvesWhenExplicitlySelected() {
        enabledRows.set(List.of(ownKeyRow()));

        AiModelConfig resolved = service.resolveUserModel("u1", "TEXT", "umk-1");

        assertNotNull(resolved);
        assertTrue(UserAiModelSupport.isUserKeyModel(resolved));
        assertThrows(BizException.class, () -> service.resolveUserModel("u1", "TEXT", "umk-missing"));
    }

    @Test
    void updateModeCanNoLongerEnableExclusiveMode() {
        UserContext.setUserId("u1");
        UpdateUserAiKeyModeRequest req = new UpdateUserAiKeyModeRequest();
        req.setOnlyUserKey(1);

        assertEquals(Integer.valueOf(0), service.updateMode(req).getOnlyUserKey());
    }

    private static UserAiModelKey ownKeyRow() {
        UserAiModelKey row = new UserAiModelKey();
        row.setKeyId("umk-1");
        row.setUserId("u1");
        row.setModalityType("TEXT");
        row.setAbilityType("TEXT");
        row.setProviderCode("XAI");
        row.setModelCode("grok-4.7");
        row.setModelName("Grok 4.7");
        row.setBaseUrl("https://api.x.ai/v1");
        row.setApiKey("xai-test-key");
        row.setEnabled(1);
        row.setDefaultModel(1);
        row.setDelFlag(0);
        return row;
    }

    /** Inject when the field exists; fields removed with the mode machinery are skipped. */
    private static void injectByType(Object target, Class<?> fieldType, Object value) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType().equals(fieldType)) {
                    field.setAccessible(true);
                    try {
                        field.set(target, value);
                    } catch (IllegalAccessException ex) {
                        throw new IllegalStateException(ex);
                    }
                    return;
                }
            }
        }
    }
}
