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
import com.bgssai.shortai.useraimodel.dto.SavedCredentialRequest;
import com.bgssai.shortai.useraimodel.dto.ToggleUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UpdateUserAiModelKeyRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyIdRequest;
import com.bgssai.shortai.useraimodel.dto.UserAiModelKeyItem;

class NamedModelKeyTest {
    private JdbcTemplate db;
    private final UserAiModelKeyService service = new UserAiModelKeyService();

    @BeforeEach
    void setUp() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new JdbcTemplate(ds).execute("""
                CREATE TABLE user_ai_model_key (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, key_id VARCHAR(64) UNIQUE, credential_id VARCHAR(64), key_name VARCHAR(64),
                    user_id VARCHAR(64), modality_type VARCHAR(16), ability_type VARCHAR(16), provider_code VARCHAR(32),
                    model_code VARCHAR(128), model_name VARCHAR(128), base_url VARCHAR(255), api_key TEXT, secret_key TEXT,
                    is_enabled INT, is_default_model INT, last_check_status VARCHAR(16), last_check_time TIMESTAMP,
                    del_flag INT, creator VARCHAR(64), modifier VARCHAR(64), gmt_create TIMESTAMP, gmt_modified TIMESTAMP)
                """);
        db = new JdbcTemplate(ds);
        db.execute("create table user_ai_setting (user_id varchar(64) primary key, creator varchar(64), modifier varchar(64))");
        db.execute("""
                create table user_ai_saved_credential (id bigint auto_increment primary key, credential_id varchar(64) unique, user_id varchar(64), provider_code varchar(32),
                key_name varchar(64), base_url varchar(255), api_key text, secret_key text, is_active tinyint, last_check_status varchar(16),
                last_check_time timestamp, gmt_create timestamp, gmt_modified timestamp, del_flag int default 0, creator varchar(64), modifier varchar(64), unique(user_id, provider_code, is_active))
                """);
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("mybatis/mapper/UserAiModelKeyMapper.xml"),
                new ClassPathResource("mybatis/mapper/UserAiSavedCredentialMapper.xml"));
        var mapper = new SqlSessionTemplate(factory.getObject()).getMapper(UserAiModelKeyMapper.class);
        var saved = new SqlSessionTemplate(factory.getObject()).getMapper(com.bgssai.shortai.mapper.UserAiSavedCredentialMapper.class);
        ReflectionTestUtils.setField(service, "savedCredentialMapper", saved);
        ReflectionTestUtils.setField(service, "registry", new SavedCredentialRegistry(saved, mapper));
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

    @Test
    void selectingProviderKeyRebindsTheSameModelAcrossAbilitiesAndInvalidatesRetiredReferences() {
        var first = createSaved("primary", "sk-a-shared-mask-key");
        var second = createSaved("spare", "sk-a-different-mask-key");
        assertTrue(first.getActive());
        assertFalse(second.getActive());

        var model = request(null, null);
        model.setItems(List.of(model("TEXT", "story"), model("IMAGE", "poster")));
        var before = service.create(model).getKeyList();
        assertTrue(before.stream().allMatch(row -> first.getCredentialId().equals(row.getCredentialId())));
        var useSecond = credentialId(second.getCredentialId());
        service.activateCredential(useSecond);
        var after = service.listEnabledRows("user-1", null);
        assertEquals(2, after.size());
        assertTrue(after.stream().allMatch(row -> second.getCredentialId().equals(row.getCredentialId())));
        for (var row : before) {
            assertThrows(BizException.class, () -> service.requirePlainApiKey("user-1", row.getKeyId()));
        }
        assertThrows(BizException.class, () -> service.deleteCredential(useSecond));
        service.activateCredential(credentialId(first.getCredentialId()));
        for (var row : after) assertThrows(BizException.class, () -> service.requirePlainApiKey("user-1", row.getKeyId()));
        service.deleteCredential(useSecond);
        assertTrue(service.listForCurrentUser(null, null).getCredentialList().stream()
                .noneMatch(c -> second.getCredentialId().equals(c.getCredentialId())));
    }

    @Test
    void legacyImportPreservesMixedCredentialsAndLegacyUmksUntilTheOwnerChooses() {
        String insert = "insert into user_ai_model_key (user_id,key_id,provider_code,base_url,key_name,api_key,"
                + "ability_type,modality_type,model_code,is_enabled,is_default_model,del_flag) "
                + "values ('user-1',?,'OPENAI','https://192.0.2.10','Shared',?,?,?,?,1,1,0)";
        db.update(insert, "umk-legacy-text", "same-secret-AAAA-key", "TEXT", "TEXT", "story");
        db.update(insert, "umk-legacy-image", "same-secret-BBBB-key", "IMAGE", "IMAGE", "frame");
        db.update(insert, "umk-legacy-music", "same-secret-AAAA-key", "MUSIC", "AUDIO", "score");
        var snapshot = service.listForCurrentUser(null, null);
        assertEquals(2, snapshot.getCredentialList().size());
        assertTrue(snapshot.getCredentialList().stream().noneMatch(c -> c.getActive()));
        assertEquals(1, snapshot.getCredentialList().stream().map(UserAiModelKeyItem::getApiKeyMasked).distinct().count());
        assertEquals("same-secret-AAAA-key", service.requirePlainApiKey("user-1", "umk-legacy-music"));
        assertEquals("same-secret-BBBB-key", service.requirePlainApiKey("user-1", "umk-legacy-image"));
        assertThrows(BizException.class, () -> service.create(request(null, null)));
        String selected = listed("umk-legacy-text").getCredentialId();
        UserContext.setUserId("other-user");
        assertThrows(BizException.class, () -> service.activateCredential(credentialId(selected)));
        assertThrows(BizException.class, () -> service.updateCredential(credentialId(selected)));
        assertThrows(BizException.class, () -> service.deleteCredential(credentialId(selected)));
        assertThrows(BizException.class, () -> service.testCredential(credentialId(selected)));
        UserContext.setUserId("user-1");
        service.activateCredential(credentialId(selected));
        assertEquals("same-secret-AAAA-key", service.requirePlainApiKey("user-1", "umk-legacy-text"));
        assertThrows(BizException.class, () -> service.requirePlainApiKey("user-1", "umk-legacy-image"));
        assertEquals(3, service.listEnabledRows("user-1", null).size());
    }

    @Test
    void credentialRotationReplacesOnlyItsBindingsAndRejectsMaskedInput() {
        var old = service.create(request("old", "sk-old-private-token")).getKeyList().getFirst();
        var unrelated = createSaved("another", "sk-unrelated-token");
        var update = credentialId(old.getCredentialId());
        update.setKeyName("renamed");
        update.setApiKey("");
        service.updateCredential(update);
        assertEquals("renamed", listed(old.getKeyId()).getKeyName());
        assertEquals("sk-old-private-token", service.requirePlainApiKey("user-1", old.getKeyId()));
        update.setApiKey(old.getApiKeyMasked());
        assertThrows(BizException.class, () -> service.updateCredential(update));
        update.setApiKey("sk-rotated-private-token");
        service.updateCredential(update);
        var current = service.listEnabledRows("user-1", "TEXT").getFirst();
        assertEquals("sk-rotated-private-token", service.requirePlainApiKey("user-1", current.getKeyId()));
        assertThrows(BizException.class, () -> service.requirePlainApiKey("user-1", old.getKeyId()));
        assertEquals("", db.queryForObject("select api_key from user_ai_model_key where key_id=?", String.class, old.getKeyId()));
        assertTrue(service.deleteCredential(credentialId(unrelated.getCredentialId())));
    }

    @Test
    void groupRemovalCleansDuplicateRowsAndTheLastUnusedProviderKeyCanBeDeleted() {
        var first = service.create(request("A", "sk-a")).getKeyList().getFirst();
        var duplicate = request("B", "sk-b");
        duplicate.setActivateOnCreate(false);
        var second = service.create(duplicate).getKeyList().getFirst();
        var edit = new UpdateUserAiModelKeyRequest();
        edit.setKeyId(second.getKeyId());
        edit.setProviderModelSelection(true);
        edit.setModelName("Shared model");
        service.update(edit);
        assertEquals("Shared model", listed(first.getKeyId()).getModelName());
        var delete = new UserAiModelKeyIdRequest();
        delete.setProviderModelSelection(true);
        delete.setKeyId(first.getKeyId());
        service.delete(delete);
        assertEquals(0, db.queryForObject("select count(*) from user_ai_model_key where del_flag=0", Integer.class));
        service.deleteCredential(credentialId(first.getCredentialId()));
        service.deleteCredential(credentialId(second.getCredentialId()));
        assertTrue(service.listForCurrentUser(null, null).getCredentialList().isEmpty());
    }

    @Test
    void explicitlySelectedTokenHubProvidersUseTheirOwnStoredKey() {
        // Use fixed DNS results so the test never depends on a public resolver.
        try (var support = org.mockito.Mockito.mockStatic(UserAiModelSupport.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            support.when(() -> UserAiModelSupport.validatePublicHttpsUrl(
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("base_url"))).thenAnswer(call -> null);
            var req = new SavedCredentialRequest();
            req.setProviderCode("BGSSAI_TOKENHUB");
            req.setApiKey("company-test-key");
            var company = service.createCredential(req);
            req.setProviderCode("TENCENT_TOKENHUB");
            req.setApiKey("tencent-test-key");
            var tencent = service.createCredential(req);
            assertEquals("TENCENT_TOKENHUB", tencent.getProviderCode());
            assertEquals("BGSSAI_TOKENHUB", company.getProviderCode());
            var selection = request(null, null);
            selection.setProviderCode("TENCENT_TOKENHUB");
            var selected = service.create(selection).getKeyList().getFirst();
            assertEquals(tencent.getCredentialId(), selected.getCredentialId());
            assertEquals("tencent-test-key", service.requirePlainApiKey("user-1", selected.getKeyId()));
            selection.setProviderCode("BGSSAI_TOKENHUB");
            selected = service.create(selection).getKeyList().getFirst();
            assertEquals(company.getCredentialId(), selected.getCredentialId());
            assertEquals("company-test-key", service.requirePlainApiKey("user-1", selected.getKeyId()));
        }
    }

    private UserAiModelKeyItem createSaved(String name, String secret) {
        var req = new SavedCredentialRequest();
        req.setProviderCode("OPENAI");
        req.setBaseUrl("https://192.0.2.10");
        req.setKeyName(name);
        req.setApiKey(secret);
        return service.createCredential(req);
    }

    private static SavedCredentialRequest credentialId(String id) {
        var req = new SavedCredentialRequest();
        req.setCredentialId(id);
        return req;
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
