package com.bgssai.shortai.useraimodel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.UserAiProviderKeyMapper;
import com.bgssai.shortai.model.UserAiProviderKey;
import com.bgssai.shortai.model.UserAiProviderKeyExample;
import com.bgssai.shortai.useraimodel.controller.UserAiKeysApiController;
import com.bgssai.shortai.useraimodel.controller.UserAiKeysController;
import com.bgssai.shortai.useraimodel.dto.UpsertUserAiProviderKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiProviderKeyItem;
import com.bgssai.shortai.useraimodel.service.UserAiProviderKeyService.ResolvedProviderKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

class UserAiProviderKeyServiceTest {

    private Map<String, UserAiProviderKey> store;
    private AtomicLong idSeq;
    private UserAiProviderKeyService service;
    private MockMvc mockMvc;
    private ObjectMapper snakeMapper;

    @BeforeEach
    void setUp() {
        store = new LinkedHashMap<>();
        idSeq = new AtomicLong(1);
        UserAiProviderKeyMapper mapper = mock(UserAiProviderKeyMapper.class);

        when(mapper.insertSelective(any(UserAiProviderKey.class))).thenAnswer(inv -> {
            UserAiProviderKey row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId(idSeq.getAndIncrement());
            }
            store.put(row.getUserId() + ":" + row.getProviderCode(), cloneRow(row));
            return 1;
        });

        when(mapper.updateByPrimaryKey(any(UserAiProviderKey.class))).thenAnswer(inv -> {
            UserAiProviderKey row = inv.getArgument(0);
            store.put(row.getUserId() + ":" + row.getProviderCode(), cloneRow(row));
            return 1;
        });

        when(mapper.selectByExample(any(UserAiProviderKeyExample.class))).thenAnswer(inv -> {
            UserAiProviderKeyExample ex = inv.getArgument(0);
            return filterRows(ex);
        });

        when(mapper.selectOneByExample(any(UserAiProviderKeyExample.class))).thenAnswer(inv -> {
            UserAiProviderKeyExample ex = inv.getArgument(0);
            List<UserAiProviderKey> hits = filterRows(ex);
            return hits.isEmpty() ? null : hits.get(0);
        });

        when(mapper.deleteByExample(any(UserAiProviderKeyExample.class))).thenAnswer(inv -> {
            UserAiProviderKeyExample ex = inv.getArgument(0);
            List<UserAiProviderKey> hits = filterRows(ex);
            for (UserAiProviderKey row : hits) {
                store.remove(row.getUserId() + ":" + row.getProviderCode());
            }
            return hits.size();
        });

        service = new UserAiProviderKeyService(mapper);

        snakeMapper = new ObjectMapper();
        snakeMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(snakeMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UserAiKeysController(service),
                new UserAiKeysApiController(service))
                .setMessageConverters(converter)
                .build();

        UserContext.setUserId("u-demo-0001");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listsAllElevenSupportedProvidersUnconfiguredByDefault() {
        List<UserAiProviderKeyItem> items = service.listForCurrentUser();
        assertEquals(11, items.size());
        List<String> codes = items.stream().map(UserAiProviderKeyItem::getProviderCode).toList();
        assertEquals(List.of(
                "DEEPSEEK", "MINIMAX", "KLING", "DASHSCOPE", "ZHIPU",
                "MOONSHOT", "VOLCENGINE", "HUNYUAN", "OPENAI", "ANTHROPIC", "GOOGLE"), codes);
        assertTrue(items.stream().noneMatch(UserAiProviderKeyItem::isConfigured));
        assertTrue(items.stream().noneMatch(UserAiProviderKeyItem::isEnabled));
    }

    @Test
    void supportsKlingAliasAndBothBearerAndAkSkJwtModes() throws Exception {
        UpsertUserAiProviderKeyRequest bearerReq = new UpsertUserAiProviderKeyRequest();
        bearerReq.setProviderCode(" klingai ");
        bearerReq.setApiKey("  kling-bearer-secret-998877  ");
        bearerReq.setBaseUrl(" https://api.klingai.com/ ");

        UserAiProviderKeyItem savedBearer = service.upsert(bearerReq);
        assertEquals("KLING", savedBearer.getProviderCode());
        assertEquals("API_KEY", savedBearer.getAuthMode());
        assertTrue(savedBearer.isConfigured());
        assertTrue(savedBearer.isEnabled());
        assertFalse(savedBearer.isHasSecretKey());
        assertEquals("https://api.klingai.com", savedBearer.getBaseUrl());
        assertNotNull(savedBearer.getMaskedKey());
        assertFalse(savedBearer.getMaskedKey().contains("998877"));

        Optional<ResolvedProviderKey> activeBearer = service.findActiveKey("u-demo-0001", "KLINGAI");
        assertTrue(activeBearer.isPresent());
        assertEquals("kling-bearer-secret-998877", activeBearer.get().resolveKlingBearerToken());
        assertEquals("https://api.klingai.com", activeBearer.get().effectiveBaseUrl());

        UpsertUserAiProviderKeyRequest akSkReq = new UpsertUserAiProviderKeyRequest();
        akSkReq.setProviderCode("KLING");
        akSkReq.setAccessKey("ak-test-12345678");
        akSkReq.setSecretKey("sk-secret-87654321");
        akSkReq.setBaseUrl("https://api-beijing.klingai.com");

        UserAiProviderKeyItem savedAkSk = service.upsert(akSkReq);
        assertEquals("KLING", savedAkSk.getProviderCode());
        assertEquals("AK_SK", savedAkSk.getAuthMode());
        assertTrue(savedAkSk.isHasSecretKey());
        assertNotNull(savedAkSk.getMaskedAccessKey());
        assertTrue(savedAkSk.getMaskedAccessKey().contains("****"));

        Optional<ResolvedProviderKey> activeJwt = service.findActiveKey("u-demo-0001", "KLING");
        assertTrue(activeJwt.isPresent());
        String jwt = activeJwt.get().resolveKlingBearerToken();
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);
        assertEquals(hmacSha256Base64Url(parts[0] + "." + parts[1], "sk-secret-87654321"), parts[2]);

        Map<String, Object> deleted = service.deleteByProviderCode("klingai");
        assertEquals("KLING", deleted.get("provider_code"));
        assertEquals(true, deleted.get("deleted"));
        assertTrue(service.findActiveKey("u-demo-0001", "KLING").isEmpty());
    }

    @Test
    void trimsInputsRejectsMasksAndValidatesBaseUrlOverrides() {
        UpsertUserAiProviderKeyRequest masked = new UpsertUserAiProviderKeyRequest();
        masked.setProviderCode("MINIMAX");
        masked.setApiKey("sk-ab****7890");
        assertThrows(BizException.class, () -> service.upsert(masked));

        UpsertUserAiProviderKeyRequest httpUrl = new UpsertUserAiProviderKeyRequest();
        httpUrl.setProviderCode("MINIMAX");
        httpUrl.setApiKey("mm-key-12345678");
        httpUrl.setBaseUrl("http://api.minimaxi.com");
        assertThrows(BizException.class, () -> service.upsert(httpUrl));

        UpsertUserAiProviderKeyRequest localUrl = new UpsertUserAiProviderKeyRequest();
        localUrl.setProviderCode("MINIMAX");
        localUrl.setApiKey("mm-key-12345678");
        localUrl.setBaseUrl("https://127.0.0.1/v1");
        assertThrows(BizException.class, () -> service.upsert(localUrl));

        UpsertUserAiProviderKeyRequest ioReq = new UpsertUserAiProviderKeyRequest();
        ioReq.setProviderCode("MINIMAX");
        ioReq.setApiKey("  mm-global-key-123456  ");
        ioReq.setBaseUrl(" https://api.minimax.io/ ");
        UserAiProviderKeyItem saved = service.upsert(ioReq);
        assertEquals("https://api.minimax.io", saved.getBaseUrl());
        assertEquals("mm-global-key-123456",
                service.findActiveKey("u-demo-0001", "MINIMAX").orElseThrow().apiKey());

        UpsertUserAiProviderKeyRequest toggleOff = new UpsertUserAiProviderKeyRequest();
        toggleOff.setProviderCode("MINIMAX");
        toggleOff.setEnabled(false);
        UserAiProviderKeyItem disabled = service.upsert(toggleOff);
        assertFalse(disabled.isEnabled());
        assertEquals("https://api.minimax.io", disabled.getBaseUrl());
        assertTrue(service.findActiveKey("u-demo-0001", "MINIMAX").isEmpty());

        UpsertUserAiProviderKeyRequest clearUrl = new UpsertUserAiProviderKeyRequest();
        clearUrl.setProviderCode("MINIMAX");
        clearUrl.setEnabled(true);
        clearUrl.setBaseUrl("");
        UserAiProviderKeyItem cleared = service.upsert(clearUrl);
        assertTrue(cleared.isEnabled());
        assertNull(cleared.getBaseUrl());
        assertEquals("https://api.minimaxi.com",
                service.findActiveKey("u-demo-0001", "MINIMAX").orElseThrow().effectiveBaseUrl());
    }

    @Test
    void savesProbesPreservesAndClearsKeyNameAndPlatformKeyName() throws Exception {
        PlatformKeyNameProber mockProber = mock(PlatformKeyNameProber.class);
        when(mockProber.probe("OPENAI", "https://www.bgssai-tokenhub.cn/v1", "sk-th-user-123456"))
                .thenReturn("user-tokenhub-key");
        UserAiProviderKeyMapper mapper = mock(UserAiProviderKeyMapper.class);
        when(mapper.insertSelective(any(UserAiProviderKey.class))).thenAnswer(inv -> {
            UserAiProviderKey row = inv.getArgument(0);
            if (row.getId() == null) row.setId(idSeq.getAndIncrement());
            store.put(row.getUserId() + ":" + row.getProviderCode(), cloneRow(row));
            return 1;
        });
        when(mapper.updateByPrimaryKey(any(UserAiProviderKey.class))).thenAnswer(inv -> {
            UserAiProviderKey row = inv.getArgument(0);
            store.put(row.getUserId() + ":" + row.getProviderCode(), cloneRow(row));
            return 1;
        });
        when(mapper.selectByExample(any(UserAiProviderKeyExample.class))).thenAnswer(inv -> filterRows(inv.getArgument(0)));
        when(mapper.selectOneByExample(any(UserAiProviderKeyExample.class))).thenAnswer(inv -> {
            List<UserAiProviderKey> hits = filterRows(inv.getArgument(0));
            return hits.isEmpty() ? null : hits.get(0);
        });
        when(mapper.deleteByExample(any(UserAiProviderKeyExample.class))).thenAnswer(inv -> {
            List<UserAiProviderKey> hits = filterRows(inv.getArgument(0));
            for (UserAiProviderKey row : hits) store.remove(row.getUserId() + ":" + row.getProviderCode());
            return hits.size();
        });

        UserAiProviderKeyService customService = new UserAiProviderKeyService(mapper, mockProber);
        UpsertUserAiProviderKeyRequest createReq = new UpsertUserAiProviderKeyRequest();
        createReq.setProviderCode("OPENAI");
        createReq.setApiKey("sk-th-user-123456");
        createReq.setBaseUrl("https://www.bgssai-tokenhub.cn/v1");
        createReq.setKeyName("  Gateway Primary  ");

        UserAiProviderKeyItem created = customService.upsert(createReq);
        assertEquals("Gateway Primary", created.getKeyName());
        assertEquals("user-tokenhub-key", created.getPlatformKeyName());

        // Toggle without api_key or key_name must preserve both
        UpsertUserAiProviderKeyRequest toggleReq = new UpsertUserAiProviderKeyRequest();
        toggleReq.setProviderCode("OPENAI");
        toggleReq.setEnabled(false);
        UserAiProviderKeyItem toggled = customService.upsert(toggleReq);
        assertEquals("Gateway Primary", toggled.getKeyName());
        assertEquals("user-tokenhub-key", toggled.getPlatformKeyName());

        // Clear key_name explicitly with blank while omitting api_key still preserves platform_key_name
        UpsertUserAiProviderKeyRequest clearNameReq = new UpsertUserAiProviderKeyRequest();
        clearNameReq.setProviderCode("OPENAI");
        clearNameReq.setKeyName("   ");
        UserAiProviderKeyItem clearedName = customService.upsert(clearNameReq);
        assertNull(clearedName.getKeyName());
        assertEquals("user-tokenhub-key", clearedName.getPlatformKeyName());

        // Delete removes both key_name and platform_key_name
        customService.deleteByProviderCode("OPENAI");
        UserAiProviderKeyItem afterDelete = customService.listForCurrentUser().stream()
                .filter(i -> "OPENAI".equals(i.getProviderCode())).findFirst().orElseThrow();
        assertNull(afterDelete.getKeyName());
        assertNull(afterDelete.getPlatformKeyName());

        // Key name > 64 chars fails validation
        UpsertUserAiProviderKeyRequest tooLong = new UpsertUserAiProviderKeyRequest();
        tooLong.setProviderCode("DEEPSEEK");
        tooLong.setApiKey("sk-ds-12345678");
        tooLong.setKeyName("x".repeat(65));
        assertThrows(BizException.class, () -> customService.upsert(tooLong));
    }

    @Test
    void controllersNeverReturnPlaintextOverApiOrBgssaiPrefixes() throws Exception {
        String payload = """
                {
                  "provider_code": "DEEPSEEK",
                  "api_key": "  sk-deepseek-super-secret-value  ",
                  "base_url": "https://api.deepseek.com"
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.post("/api/user/ai-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.provider_code").value("DEEPSEEK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.configured").value(true));

        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/bgssai/user/ai-keys"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.items.length()").value(11))
                .andReturn();
        String json = getResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(json.contains("sk-deepseek-super-secret-value"), "GET must never leak plaintext key");
        assertTrue(json.contains("sk-d****alue"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/user/ai-keys/DEEPSEEK"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.deleted").value(true));
    }

    private List<UserAiProviderKey> filterRows(UserAiProviderKeyExample ex) {
        String userFilter = null;
        String providerFilter = null;
        Byte enabledFilter = null;
        if (ex != null && !ex.getOredCriteria().isEmpty()) {
            for (UserAiProviderKeyExample.Criterion c : ex.getOredCriteria().get(0).getCriteria()) {
                if ("user_id =".equals(c.getCondition())) {
                    userFilter = String.valueOf(c.getValue());
                } else if ("provider_code =".equals(c.getCondition())) {
                    providerFilter = String.valueOf(c.getValue());
                } else if ("enabled =".equals(c.getCondition())) {
                    enabledFilter = ((Number) c.getValue()).byteValue();
                }
            }
        }
        List<UserAiProviderKey> out = new ArrayList<>();
        for (UserAiProviderKey row : store.values()) {
            if (userFilter != null && !userFilter.equals(row.getUserId())) {
                continue;
            }
            if (providerFilter != null && !providerFilter.equals(row.getProviderCode())) {
                continue;
            }
            if (enabledFilter != null && !enabledFilter.equals(row.getEnabled())) {
                continue;
            }
            out.add(cloneRow(row));
        }
        return out;
    }

    private static UserAiProviderKey cloneRow(UserAiProviderKey src) {
        UserAiProviderKey copy = new UserAiProviderKey();
        copy.setId(src.getId());
        copy.setUserId(src.getUserId());
        copy.setProviderCode(src.getProviderCode());
        copy.setApiKey(src.getApiKey());
        copy.setAccessKey(src.getAccessKey());
        copy.setSecretKey(src.getSecretKey());
        copy.setBaseUrl(src.getBaseUrl());
        copy.setKeyName(src.getKeyName());
        copy.setPlatformKeyName(src.getPlatformKeyName());
        copy.setEnabled(src.getEnabled());
        copy.setCreatedAt(src.getCreatedAt());
        copy.setUpdatedAt(src.getUpdatedAt());
        return copy;
    }

    private static String hmacSha256Base64Url(String signingInput, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }
}
