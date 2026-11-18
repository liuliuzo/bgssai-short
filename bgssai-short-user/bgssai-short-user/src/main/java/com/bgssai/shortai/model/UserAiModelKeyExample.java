package com.bgssai.shortai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserAiModelKeyExample {
    protected String orderByClause;
    protected boolean distinct;
    protected List<Criteria> oredCriteria;
    private String[] selectColumns;

    public UserAiModelKeyExample() {
        oredCriteria = new ArrayList<>();
    }

    public UserAiModelKeyExample selectColumns(String... cols) {
        this.selectColumns = cols;
        return this;
    }

    public String[] getSelectColumns() { return selectColumns; }
    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }
    public String getOrderByClause() { return orderByClause; }
    public void setDistinct(boolean distinct) { this.distinct = distinct; }
    public boolean isDistinct() { return distinct; }
    public List<Criteria> getOredCriteria() { return oredCriteria; }
    public void or(Criteria criteria) { oredCriteria.add(criteria); }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.isEmpty()) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() { return new Criteria(); }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
        selectColumns = null;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() { criteria = new ArrayList<>(); }

        public boolean isValid() { return !criteria.isEmpty(); }
        public List<Criterion> getAllCriteria() { return criteria; }
        public List<Criterion> getCriteria() { return criteria; }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() { addCriterion("id is null"); return (Criteria) this; }
        public Criteria andIdIsNotNull() { addCriterion("id is not null"); return (Criteria) this; }
        public Criteria andIdEqualTo(Long value) { addCriterion("id =", value, "id"); return (Criteria) this; }
        public Criteria andIdNotEqualTo(Long value) { addCriterion("id <>", value, "id"); return (Criteria) this; }
        public Criteria andIdIn(List<Long> values) { addCriterion("id in", values, "id"); return (Criteria) this; }

        public Criteria andKeyIdIsNull() { addCriterion("key_id is null"); return (Criteria) this; }
        public Criteria andKeyIdIsNotNull() { addCriterion("key_id is not null"); return (Criteria) this; }
        public Criteria andKeyIdEqualTo(String value) { addCriterion("key_id =", value, "keyId"); return (Criteria) this; }
        public Criteria andKeyIdNotEqualTo(String value) { addCriterion("key_id <>", value, "keyId"); return (Criteria) this; }
        public Criteria andKeyIdIn(List<String> values) { addCriterion("key_id in", values, "keyId"); return (Criteria) this; }

        public Criteria andUserIdIsNull() { addCriterion("user_id is null"); return (Criteria) this; }
        public Criteria andUserIdIsNotNull() { addCriterion("user_id is not null"); return (Criteria) this; }
        public Criteria andUserIdEqualTo(String value) { addCriterion("user_id =", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdNotEqualTo(String value) { addCriterion("user_id <>", value, "userId"); return (Criteria) this; }
        public Criteria andUserIdIn(List<String> values) { addCriterion("user_id in", values, "userId"); return (Criteria) this; }

        public Criteria andModalityTypeIsNull() { addCriterion("modality_type is null"); return (Criteria) this; }
        public Criteria andModalityTypeIsNotNull() { addCriterion("modality_type is not null"); return (Criteria) this; }
        public Criteria andModalityTypeEqualTo(String value) { addCriterion("modality_type =", value, "modalityType"); return (Criteria) this; }
        public Criteria andModalityTypeNotEqualTo(String value) { addCriterion("modality_type <>", value, "modalityType"); return (Criteria) this; }
        public Criteria andModalityTypeIn(List<String> values) { addCriterion("modality_type in", values, "modalityType"); return (Criteria) this; }

        public Criteria andAbilityTypeIsNull() { addCriterion("ability_type is null"); return (Criteria) this; }
        public Criteria andAbilityTypeIsNotNull() { addCriterion("ability_type is not null"); return (Criteria) this; }
        public Criteria andAbilityTypeEqualTo(String value) { addCriterion("ability_type =", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeNotEqualTo(String value) { addCriterion("ability_type <>", value, "abilityType"); return (Criteria) this; }
        public Criteria andAbilityTypeIn(List<String> values) { addCriterion("ability_type in", values, "abilityType"); return (Criteria) this; }

        public Criteria andProviderCodeIsNull() { addCriterion("provider_code is null"); return (Criteria) this; }
        public Criteria andProviderCodeIsNotNull() { addCriterion("provider_code is not null"); return (Criteria) this; }
        public Criteria andProviderCodeEqualTo(String value) { addCriterion("provider_code =", value, "providerCode"); return (Criteria) this; }
        public Criteria andProviderCodeNotEqualTo(String value) { addCriterion("provider_code <>", value, "providerCode"); return (Criteria) this; }

        public Criteria andModelCodeIsNull() { addCriterion("model_code is null"); return (Criteria) this; }
        public Criteria andModelCodeIsNotNull() { addCriterion("model_code is not null"); return (Criteria) this; }
        public Criteria andModelCodeEqualTo(String value) { addCriterion("model_code =", value, "modelCode"); return (Criteria) this; }
        public Criteria andModelCodeNotEqualTo(String value) { addCriterion("model_code <>", value, "modelCode"); return (Criteria) this; }

        public Criteria andModelNameIsNull() { addCriterion("model_name is null"); return (Criteria) this; }
        public Criteria andModelNameIsNotNull() { addCriterion("model_name is not null"); return (Criteria) this; }
        public Criteria andModelNameEqualTo(String value) { addCriterion("model_name =", value, "modelName"); return (Criteria) this; }

        public Criteria andCredentialIdIsNull() { addCriterion("credential_id is null"); return (Criteria) this; }
        public Criteria andCredentialIdEqualTo(String value) { addCriterion("credential_id =", value, "credentialId"); return (Criteria) this; }

        public Criteria andKeyNameIsNull() { addCriterion("key_name is null"); return (Criteria) this; }
        public Criteria andKeyNameIsNotNull() { addCriterion("key_name is not null"); return (Criteria) this; }
        public Criteria andKeyNameEqualTo(String value) { addCriterion("key_name =", value, "keyName"); return (Criteria) this; }

        public Criteria andBaseUrlIsNull() { addCriterion("base_url is null"); return (Criteria) this; }
        public Criteria andBaseUrlIsNotNull() { addCriterion("base_url is not null"); return (Criteria) this; }
        public Criteria andBaseUrlEqualTo(String value) { addCriterion("base_url =", value, "baseUrl"); return (Criteria) this; }

        public Criteria andSecretKeyIsNull() { addCriterion("secret_key is null"); return (Criteria) this; }
        public Criteria andSecretKeyIsNotNull() { addCriterion("secret_key is not null"); return (Criteria) this; }

        public Criteria andEnabledIsNull() { addCriterion("is_enabled is null"); return (Criteria) this; }
        public Criteria andEnabledIsNotNull() { addCriterion("is_enabled is not null"); return (Criteria) this; }
        public Criteria andEnabledEqualTo(Integer value) { addCriterion("is_enabled =", value, "enabled"); return (Criteria) this; }
        public Criteria andEnabledNotEqualTo(Integer value) { addCriterion("is_enabled <>", value, "enabled"); return (Criteria) this; }

        public Criteria andDefaultModelIsNull() { addCriterion("is_default_model is null"); return (Criteria) this; }
        public Criteria andDefaultModelIsNotNull() { addCriterion("is_default_model is not null"); return (Criteria) this; }
        public Criteria andDefaultModelEqualTo(Integer value) { addCriterion("is_default_model =", value, "defaultModel"); return (Criteria) this; }
        public Criteria andDefaultModelNotEqualTo(Integer value) { addCriterion("is_default_model <>", value, "defaultModel"); return (Criteria) this; }

        public Criteria andLastCheckStatusIsNull() { addCriterion("last_check_status is null"); return (Criteria) this; }
        public Criteria andLastCheckStatusIsNotNull() { addCriterion("last_check_status is not null"); return (Criteria) this; }
        public Criteria andLastCheckStatusEqualTo(String value) { addCriterion("last_check_status =", value, "lastCheckStatus"); return (Criteria) this; }

        public Criteria andLastCheckTimeIsNull() { addCriterion("last_check_time is null"); return (Criteria) this; }
        public Criteria andLastCheckTimeIsNotNull() { addCriterion("last_check_time is not null"); return (Criteria) this; }
        public Criteria andLastCheckTimeEqualTo(Date value) { addCriterion("last_check_time =", value, "lastCheckTime"); return (Criteria) this; }

        public Criteria andDelFlagIsNull() { addCriterion("del_flag is null"); return (Criteria) this; }
        public Criteria andDelFlagIsNotNull() { addCriterion("del_flag is not null"); return (Criteria) this; }
        public Criteria andDelFlagEqualTo(Integer value) { addCriterion("del_flag =", value, "delFlag"); return (Criteria) this; }
        public Criteria andDelFlagNotEqualTo(Integer value) { addCriterion("del_flag <>", value, "delFlag"); return (Criteria) this; }

        public Criteria andCreatorIsNull() { addCriterion("creator is null"); return (Criteria) this; }
        public Criteria andCreatorIsNotNull() { addCriterion("creator is not null"); return (Criteria) this; }
        public Criteria andCreatorEqualTo(String value) { addCriterion("creator =", value, "creator"); return (Criteria) this; }

        public Criteria andModifierIsNull() { addCriterion("modifier is null"); return (Criteria) this; }
        public Criteria andModifierIsNotNull() { addCriterion("modifier is not null"); return (Criteria) this; }
        public Criteria andModifierEqualTo(String value) { addCriterion("modifier =", value, "modifier"); return (Criteria) this; }

        public Criteria andGmtCreateIsNull() { addCriterion("gmt_create is null"); return (Criteria) this; }
        public Criteria andGmtCreateIsNotNull() { addCriterion("gmt_create is not null"); return (Criteria) this; }
        public Criteria andGmtCreateEqualTo(Date value) { addCriterion("gmt_create =", value, "gmtCreate"); return (Criteria) this; }

        public Criteria andGmtModifiedIsNull() { addCriterion("gmt_modified is null"); return (Criteria) this; }
        public Criteria andGmtModifiedIsNotNull() { addCriterion("gmt_modified is not null"); return (Criteria) this; }
        public Criteria andGmtModifiedEqualTo(Date value) { addCriterion("gmt_modified =", value, "gmtModified"); return (Criteria) this; }
    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() { super(); }
    }

    public static class Criterion {
        private String condition;
        private Object value;
        private Object secondValue;
        private boolean noValue;
        private boolean singleValue;
        private boolean betweenValue;
        private boolean listValue;
        private String typeHandler;

        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public Object getSecondValue() { return secondValue; }
        public boolean isNoValue() { return noValue; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isBetweenValue() { return betweenValue; }
        public boolean isListValue() { return listValue; }
        public String getTypeHandler() { return typeHandler; }

        protected Criterion(String condition) {
            this.condition = condition;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}
