package com.bgssai.shortai.useraimodel.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.UserAiModelKeyMapper;
import com.bgssai.shortai.mapper.UserAiSettingMapper;
import com.bgssai.shortai.useraimodel.dto.CreateUserAiModelKeyItem;
import com.bgssai.shortai.useraimodel.dto.CreateUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.ToggleUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UpdateUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyIdRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyItem;

class NamedModelKeyTest {
    private final UserAiModelKeyService service = new UserAiModelKeyService();

    @BeforeEach
    void setUp() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new JdbcTemplate(ds).execute("""
                CREATE TABLE user_ai_model_key (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, key_id VARCHAR(64) UNIQUE, key_name VARCHAR(64),
                    user_id VARCHAR(64), modality_type VARCHAR(16), ability_type VARCHAR(16), provider_code VARCHAR(32),
                    model_code VARCHAR(128), model_name VARCHAR(128), base_url VARCHAR(255), api_key TEXT, secret_key TEXT,
                    is_enabled INT, is_default_model INT, last_check_status VARCHAR(16), last_check_time TIMESTAMP,
                    del_flag INT, creator VARCHAR(64), modifier VARCHAR(64), gmt_create TIMESTAMP, gmt_modified TIMESTAMP)
                """);
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("mybatis/mapper/UserAiModelKeyMapper.xml"));
        var mapper = new SqlSessionTemplate(factory.getObject()).getMapper(UserAiModelKeyMapper.class);
        ReflectionTestUtils.setField(service, "userAiModelKeyMapper", mapper);
        ReflectionTestUtils.setField(service, "userAiSettingMapper", mock(UserAiSettingMapper.class));
        ReflectionTestUtils.setField(service, "externalApiKeyBalanceClient", mock(ExternalApiKeyBalanceClient.class));
        UserContext.setUserId("user-1");
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void standbyKeyCanBeActivatedRenamedAndClearedWithoutChangingItsSibling() {
        var first = service.create(request("主用", "sk-test-primary")).getKeyList().getFirst();
        var standby = request("  备用  ", "sk-test-standby");
        standby.setActivateOnCreate(false);
        var second = service.create(standby).getKeyList().getFirst();

        assertNotEquals(first.getKeyId(), second.getKeyId());
        assertEquals("备用", second.getKeyName());
        assertEquals(0, second.getEnabled());
        assertEquals(0, second.getDefaultModel());
        assertEquals(1, listed(first.getKeyId()).getEnabled());
        assertEquals("sk-test-primary", service.requirePlainApiKey("user-1", first.getKeyId()));
        assertNotEquals("sk-test-standby", second.getApiKeyMasked());

        var use = new ToggleUserAiModelKeyRequest();
        use.setKeyId(second.getKeyId());
        use.setEnabled(1);
        assertEquals(1, service.toggle(use).getItem().getDefaultModel());
        assertEquals(0, listed(first.getKeyId()).getEnabled());
        assertEquals("sk-test-standby", service.requirePlainApiKey("user-1", second.getKeyId()));

        var edit = new UpdateUserAiModelKeyRequest();
        edit.setKeyId(second.getKeyId());
        assertEquals("备用", service.update(edit).getItem().getKeyName());
        edit.setKeyName("项目 B");
        assertEquals("项目 B", service.update(edit).getItem().getKeyName());
        edit.setKeyName("");
        assertNull(service.update(edit).getItem().getKeyName());
        assertEquals("主用", listed(first.getKeyId()).getKeyName());

        var delete = new UserAiModelKeyIdRequest();
        delete.setKeyId(first.getKeyId());
        UserContext.setUserId("other-user");
        assertThrows(BizException.class, () -> service.delete(delete));
        UserContext.setUserId("user-1");
        service.delete(delete);
        assertEquals(1, listed(second.getKeyId()).getEnabled());
    }

    @Test
    void batchStandbyLeavesEveryAbilityUnselectedAndRejectsAnOversizeName() {
        var request = request("团队备用", "sk-test-shared");
        request.setItems(List.of(model("TEXT", "one"), model("IMAGE", "two")));
        request.setActivateOnCreate(false);
        var rows = service.create(request).getKeyList();
        assertEquals(2, rows.size());
        for (var row : rows) {
            var stored = listed(row.getKeyId());
            assertEquals("团队备用", stored.getKeyName());
            assertEquals(0, stored.getEnabled());
            assertEquals(0, stored.getDefaultModel());
        }
        request.setKeyName("字".repeat(65));
        assertThrows(BizException.class, () -> service.create(request));
    }

    private UserAiModelKeyItem listed(String id) {
        return service.listForCurrentUser(null, null).getKeyList().stream()
                .filter(row -> id.equals(row.getKeyId())).findFirst().orElseThrow();
    }

    private static CreateUserAiModelKeyRequest request(String name, String secret) {
        var request = new CreateUserAiModelKeyRequest();
        request.setProviderCode("OPENAI");
        request.setBaseUrl("https://192.0.2.10");
        request.setAbilityType("TEXT");
        request.setModelCode("same-model");
        request.setApiKey(secret);
        request.setKeyName(name);
        return request;
    }

    private static CreateUserAiModelKeyItem model(String ability, String code) {
        var item = new CreateUserAiModelKeyItem();
        item.setAbilityType(ability);
        item.setModelCode(code);
        return item;
    }
}
