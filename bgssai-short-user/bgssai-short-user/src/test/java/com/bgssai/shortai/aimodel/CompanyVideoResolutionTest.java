package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.secret.SysSecretReader;

class CompanyVideoResolutionTest {
    private final AiModelConfigMapper mapper = mock(AiModelConfigMapper.class);
    private final AiProviderRegistry registry = mock(AiProviderRegistry.class);
    private final TokenHubModelCatalog catalog = mock(TokenHubModelCatalog.class);
    private final AiModelResolveService service = new AiModelResolveService();

    @BeforeEach
    void wire() {
        ReflectionTestUtils.setField(service, "aiModelConfigMapper", mapper);
        ReflectionTestUtils.setField(service, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(service, "tokenHubModelCatalog", catalog);
        when(registry.resolvable(any())).thenReturn(true);
        when(registry.supports(any())).thenReturn(true);
        when(catalog.endpointFor(any(), any())).thenReturn("https://dev.user.bgssai-tokenhub.cn");
        when(catalog.pricingFor(any(), any())).thenAnswer(invocation -> Map.of(
                "pricing_status", "configured", "price_version", "bgssai-video".equals(invocation.getArgument(1)) ? 7 : 99));
    }

    @Test
    void canonicalIgnoresThirdPartyDefaultAndRetainsLegacyMetadata() {
        AiModelConfig thirdParty = row("third-party", "minimax-video-h3", 1, 250, 120);
        AiModelConfig legacy = legacy();
        seed(thirdParty, legacy);
        AiModelConfig chosen = service.resolve("VIDEO", " bgssai-video ");
        assertNotSame(legacy, chosen);
        assertEquals("bgssai-video", chosen.getModelCode());
        assertEquals("cogvideox", legacy.getModelCode());
        assertEquals(legacy.getModelId(), chosen.getModelId());
        assertEquals(legacy.getConfigJson(), chosen.getConfigJson());
        assertEquals(900, chosen.getTimeoutSec());
        assertEquals(3, chosen.getMaxRetry());
        assertEquals(100, service.costCent(chosen, 5));
        assertCatalogMatches(chosen);
        verify(catalog, never()).snapshot();
    }

    @Test
    void blankVideoUsesEnabledCompanyDefaultWhenCatalogIsEmptyOrOnlyListsCogvideox() {
        AiModelConfig company = row("m-tokenhub-video-bgssai", "bgssai-video", 1, 20, 900);
        company.setConfigJson("{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\",\"poll_interval_ms\":3000}");
        AiModelConfig minimax = row("m-tokenhub-video-minimax", "minimax-video-h3", 0, 50, 900);
        seed(minimax, legacy(), company);

        when(catalog.snapshot()).thenReturn(List.of());
        assertSame(company, service.resolve("VIDEO", null));
        assertSame(company, service.resolve("VIDEO", " "));
        assertEquals("bgssai-video", service.resolve("VIDEO", null).getModelCode());

        when(catalog.snapshot()).thenReturn(List.of(
                new TokenHubModelCatalog.HubModel("cogvideox", List.of("video_generation"), Map.of(), "BGSSAI"),
                new TokenHubModelCatalog.HubModel("minimax-video-h3", List.of("video_generation"), Map.of(), "MiniMax")));
        AiModelConfig chosen = service.resolve("VIDEO", null);
        assertSame(company, chosen);
        assertEquals("bgssai-video", chosen.getModelCode());
        assertEquals(900, chosen.getTimeoutSec());
    }

    @Test
    void blankVideoKeepsThirdPartyDefaultWhenCompanyRowIsNotDefault() {
        AiModelConfig minimax = row("m-tokenhub-video-minimax", "minimax-video-h3", 1, 50, 900);
        seed(minimax, legacy());
        when(catalog.snapshot()).thenReturn(List.of());
        AiModelConfig chosen = service.resolve("VIDEO", null);
        assertSame(minimax, chosen);
        assertEquals("minimax-video-h3", chosen.getModelCode());
    }

    @Test
    void exactCompanyConfigWinsEvenWhenLegacyAndThirdPartyAreDefaults() {
        AiModelConfig legacy = legacy();
        legacy.setDefaultFlag(1);
        AiModelConfig exact = row("company-current", "bgssai-video", 0, 31, 777);
        seed(row("third-party", "cogvideox", 1, 250, 120), legacy, exact);
        assertSame(exact, service.resolve("VIDEO", "bgssai-video"));
        assertCatalogMatches(exact);
    }

    @Test
    void exactDuplicateSelectionIsIndependentOfCatalogQualityOrdering() {
        AiModelConfig first = row("company-first", "bgssai-video", 0, 31, 777);
        first.setId(10L);
        first.setQualityLevel("STANDARD");
        AiModelConfig second = row("company-second", "bgssai-video", 0, 42, 999);
        second.setId(11L);
        second.setQualityLevel("FAST");
        seed(second, first);
        assertSame(first, service.resolve("VIDEO", "bgssai-video"));
        assertCatalogMatches(first);
    }

    @Test
    void onlyKnownDomesticUnmodifiedLegacyIdentityIsCompatible() {
        for (String variant : List.of("id", "provider", "hub", "base", "alias", "code", "disabled", "deleted")) {
            AiModelConfig row = legacy();
            switch (variant) {
                case "id" -> row.setModelId("user-custom-cogvideox");
                case "provider" -> row.setProviderCode("OPENAI_COMPAT");
                case "hub" -> row.setConfigJson(row.getConfigJson().replace("cn", "global"));
                case "base" -> row.setConfigJson(row.getConfigJson().replace("{", "{\"base_url\":\"https://www.bgssai-tokenhub.com\","));
                case "alias" -> row.setConfigJson(row.getConfigJson().replace("bgssai-video", "bgssai-video-custom"));
                case "code" -> row.setModelCode("cogvideox-5b");
                case "disabled" -> row.setEnabledFlag(0);
                case "deleted" -> row.setDeletedFlag(1);
                default -> fail(variant);
            }
            seed(row);
            assertThrows(BizException.class, () -> service.resolve("VIDEO", "bgssai-video"), variant);
            assertTrue(items().stream().noneMatch(item -> "bgssai-video".equals(item.get("model_code"))), variant);
        }
    }

    @Test
    void missingCompanyFailsClosedEvenWithDirectCompatCredentials() {
        ReflectionTestUtils.setField(service, "secrets", new SysSecretReader(key ->
                SysSecretReader.AI_OPENAI_COMPAT_API_KEY.equals(key) ? "test-only" : ""));
        seed(row("default", "minimax-video-h3", 1, 250, 120));
        assertThrows(BizException.class, () -> service.resolve("VIDEO", "bgssai-video"));
        assertTrue(items().isEmpty());
    }

    @Test
    void explicitThirdPartyAndCustomAliasRetainOriginalRowAndPrice() {
        AiModelConfig thirdParty = row("user-custom", "cogvideox", 1, 250, 600);
        thirdParty.setConfigJson("{\"hub\":\"cn\",\"public_alias\":\"bgssai-video-custom\"}");
        seed(thirdParty, legacy());
        assertSame(thirdParty, service.resolve("VIDEO", "cogvideox"));
        assertSame(thirdParty, service.resolve("VIDEO", "bgssai-video-custom"));
        Map<String, Object> item = items().stream().filter(v -> "bgssai-video-custom".equals(v.get("model_code"))).findFirst().orElseThrow();
        assertEquals(250, item.get("unit_price_cent"));
    }

    private void assertCatalogMatches(AiModelConfig chosen) {
        List<Map<String, Object>> items = items();
        assertEquals(1, items.size());
        Map<String, Object> item = items.get(0);
        assertEquals("bgssai-video", item.get("model_code"));
        assertEquals(chosen.getPriceUnit(), item.get("price_unit"));
        assertEquals(chosen.getUnitPriceCent(), item.get("unit_price_cent"));
        assertEquals(chosen.getDefaultFlag(), item.get("default_flag"));
        assertEquals(7, item.get("price_version"));
        assertFalse(item.containsKey("config_json"));
        assertFalse(item.containsKey("provider_code"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items() {
        Map<String, Object> result = (Map<String, Object>) service.listEnabled("VIDEO").getResult();
        return (List<Map<String, Object>>) result.get("model_list");
    }

    private void seed(AiModelConfig... rows) {
        doAnswer(invocation -> {
            AiModelConfigExample example = invocation.getArgument(0);
            return java.util.Arrays.stream(rows).filter(row -> example.getOredCriteria().stream().anyMatch(criteria ->
                    criteria.getAllCriteria().stream().allMatch(c -> switch (c.getCondition()) {
                        case "ability_type =" -> java.util.Objects.equals(row.getAbilityType(), c.getValue());
                        case "model_code =" -> java.util.Objects.equals(row.getModelCode(), c.getValue());
                        case "enabled_flag =" -> java.util.Objects.equals(row.getEnabledFlag(), c.getValue());
                        case "deleted_flag =" -> java.util.Objects.equals(row.getDeletedFlag(), c.getValue());
                        case "default_flag =" -> java.util.Objects.equals(row.getDefaultFlag(), c.getValue());
                        default -> throw new AssertionError(c.getCondition());
                    }))).toList();
        }).when(mapper).selectByExample(any());
    }

    private static AiModelConfig legacy() {
        AiModelConfig row = row("m-tokenhub-video-cogvideox", "cogvideox", 0, 20, 900);
        row.setConfigJson("{\"hub\":\"cn\",\"public_alias\":\"bgssai-video\",\"secret_ref\":\"ai.tokenhub_cn.custom\",\"poll_interval_ms\":3500}");
        return row;
    }

    private static AiModelConfig row(String id, String code, int defaultFlag, int price, int timeout) {
        AiModelConfig row = new AiModelConfig();
        row.setModelId(id);
        row.setAbilityType("VIDEO");
        row.setProviderCode("TENCENT_TOKENHUB");
        row.setModelCode(code);
        row.setDefaultFlag(defaultFlag);
        row.setEnabledFlag(1);
        row.setDeletedFlag(0);
        row.setUnitPriceCent(price);
        row.setPriceUnit("PER_SECOND");
        row.setTimeoutSec(timeout);
        row.setMaxRetry(3);
        row.setConfigJson("{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\"}");
        return row;
    }
}
