"""Generate MyBatis model/example/mapper/xml for short-production tables."""
from pathlib import Path
import shutil

ROOT = Path(r"C:\Users\lzhao3730\Desktop\github\bgssai-short")
USER_JAVA = ROOT / "bgssai-short-user" / "bgssai-short-user" / "src" / "main" / "java" / "com" / "bgssai" / "shortai"
ADMIN_JAVA = ROOT / "bgssai-short-admin" / "bgssai-short-admin" / "src" / "main" / "java" / "com" / "bgssai" / "shortai"
USER_XML = ROOT / "bgssai-short-user" / "bgssai-short-user" / "src" / "main" / "resources" / "mybatis" / "mapper"
ADMIN_XML = ROOT / "bgssai-short-admin" / "bgssai-short-admin" / "src" / "main" / "resources" / "mybatis" / "mapper"

# col: (db, java, jdbc, jtype)
C = lambda db, java, jdbc, jtype: (db, java, jdbc, jtype)

TABLES = [
    ("short_work", "ShortWork", [
        C("id", "id", "BIGINT", "Long"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("work_title", "workTitle", "VARCHAR", "String"),
        C("one_line_pitch", "oneLinePitch", "VARCHAR", "String"),
        C("genre", "genre", "VARCHAR", "String"),
        C("work_status", "workStatus", "VARCHAR", "String"),
        C("episode_count", "episodeCount", "INTEGER", "Integer"),
        C("episode_duration_sec", "episodeDurationSec", "INTEGER", "Integer"),
        C("visual_style", "visualStyle", "VARCHAR", "String"),
        C("aspect_ratio", "aspectRatio", "VARCHAR", "String"),
        C("cover_asset_id", "coverAssetId", "VARCHAR", "String"),
        C("cost_budget_cent", "costBudgetCent", "INTEGER", "Integer"),
        C("cost_spent_cent", "costSpentCent", "INTEGER", "Integer"),
        C("skip_level_check_flag", "skipLevelCheckFlag", "TINYINT", "Integer"),
        C("quality_threshold_score", "qualityThresholdScore", "INTEGER", "Integer"),
        C("outline_json", "outlineJson", "LONGVARCHAR", "String"),
        C("compose_setting_json", "composeSettingJson", "LONGVARCHAR", "String"),
        C("archived_flag", "archivedFlag", "TINYINT", "Integer"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_episode", "ShortEpisode", [
        C("id", "id", "BIGINT", "Long"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("episode_no", "episodeNo", "INTEGER", "Integer"),
        C("episode_title", "episodeTitle", "VARCHAR", "String"),
        C("episode_synopsis", "episodeSynopsis", "VARCHAR", "String"),
        C("ending_hook", "endingHook", "VARCHAR", "String"),
        C("script_status", "scriptStatus", "VARCHAR", "String"),
        C("storyboard_status", "storyboardStatus", "VARCHAR", "String"),
        C("dubbing_status", "dubbingStatus", "VARCHAR", "String"),
        C("compose_status", "composeStatus", "VARCHAR", "String"),
        C("review_status", "reviewStatus", "VARCHAR", "String"),
        C("film_asset_id", "filmAssetId", "VARCHAR", "String"),
        C("film_duration_sec", "filmDurationSec", "INTEGER", "Integer"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_script", "ShortScript", [
        C("id", "id", "BIGINT", "Long"),
        C("script_id", "scriptId", "VARCHAR", "String"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("script_version", "scriptVersion", "INTEGER", "Integer"),
        C("script_status", "scriptStatus", "VARCHAR", "String"),
        C("scene_json", "sceneJson", "LONGVARCHAR", "String"),
        C("estimated_total_sec", "estimatedTotalSec", "INTEGER", "Integer"),
        C("rhythm_json", "rhythmJson", "LONGVARCHAR", "String"),
        C("total_score", "totalScore", "INTEGER", "Integer"),
        C("force_ready_flag", "forceReadyFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_script_version", "ShortScriptVersion", [
        C("id", "id", "BIGINT", "Long"),
        C("version_id", "versionId", "VARCHAR", "String"),
        C("script_id", "scriptId", "VARCHAR", "String"),
        C("script_version", "scriptVersion", "INTEGER", "Integer"),
        C("scene_json", "sceneJson", "LONGVARCHAR", "String"),
        C("total_score", "totalScore", "INTEGER", "Integer"),
        C("change_summary", "changeSummary", "VARCHAR", "String"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
    ]),
    ("short_character", "ShortCharacter", [
        C("id", "id", "BIGINT", "Long"),
        C("character_id", "characterId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("character_name", "characterName", "VARCHAR", "String"),
        C("character_role", "characterRole", "VARCHAR", "String"),
        C("card_status", "cardStatus", "VARCHAR", "String"),
        C("appearance_prompt", "appearancePrompt", "VARCHAR", "String"),
        C("consistency_seed", "consistencySeed", "BIGINT", "Long"),
        C("ref_image_asset_id", "refImageAssetId", "VARCHAR", "String"),
        C("default_outfit_code", "defaultOutfitCode", "VARCHAR", "String"),
        C("outfit_json", "outfitJson", "LONGVARCHAR", "String"),
        C("voice_id", "voiceId", "VARCHAR", "String"),
        C("voice_speed", "voiceSpeed", "VARCHAR", "String"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_scene_card", "ShortSceneCard", [
        C("id", "id", "BIGINT", "Long"),
        C("scene_card_id", "sceneCardId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("card_type", "cardType", "VARCHAR", "String"),
        C("card_name", "cardName", "VARCHAR", "String"),
        C("card_status", "cardStatus", "VARCHAR", "String"),
        C("appearance_prompt", "appearancePrompt", "VARCHAR", "String"),
        C("ref_image_asset_id", "refImageAssetId", "VARCHAR", "String"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_shot", "ShortShot", [
        C("id", "id", "BIGINT", "Long"),
        C("shot_id", "shotId", "VARCHAR", "String"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("shot_no", "shotNo", "INTEGER", "Integer"),
        C("scene_no", "sceneNo", "INTEGER", "Integer"),
        C("shot_size", "shotSize", "VARCHAR", "String"),
        C("camera_move", "cameraMove", "VARCHAR", "String"),
        C("shot_duration_sec", "shotDurationSec", "INTEGER", "Integer"),
        C("shot_desc", "shotDesc", "VARCHAR", "String"),
        C("dialogue_text", "dialogueText", "VARCHAR", "String"),
        C("emotion", "emotion", "VARCHAR", "String"),
        C("lighting", "lighting", "VARCHAR", "String"),
        C("character_ref_json", "characterRefJson", "LONGVARCHAR", "String"),
        C("scene_card_id", "sceneCardId", "VARCHAR", "String"),
        C("extra_prompt", "extraPrompt", "VARCHAR", "String"),
        C("image_prompt", "imagePrompt", "LONGVARCHAR", "String"),
        C("previz_text", "previzText", "LONGVARCHAR", "String"),
        C("shot_status", "shotStatus", "VARCHAR", "String"),
        C("blocked_flag", "blockedFlag", "TINYINT", "Integer"),
        C("risk_level", "riskLevel", "VARCHAR", "String"),
        C("first_frame_asset_id", "firstFrameAssetId", "VARCHAR", "String"),
        C("video_asset_id", "videoAssetId", "VARCHAR", "String"),
        C("retry_count", "retryCount", "INTEGER", "Integer"),
        C("cost_spent_cent", "costSpentCent", "INTEGER", "Integer"),
        C("shot_version", "shotVersion", "INTEGER", "Integer"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_ai_task", "ShortAiTask", [
        C("id", "id", "BIGINT", "Long"),
        C("task_id", "taskId", "VARCHAR", "String"),
        C("batch_id", "batchId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("task_type", "taskType", "VARCHAR", "String"),
        C("task_status", "taskStatus", "VARCHAR", "String"),
        C("target_type", "targetType", "VARCHAR", "String"),
        C("target_id", "targetId", "VARCHAR", "String"),
        C("model_code", "modelCode", "VARCHAR", "String"),
        C("request_json", "requestJson", "LONGVARCHAR", "String"),
        C("result_json", "resultJson", "LONGVARCHAR", "String"),
        C("progress_percent", "progressPercent", "INTEGER", "Integer"),
        C("retry_count", "retryCount", "INTEGER", "Integer"),
        C("fail_reason", "failReason", "VARCHAR", "String"),
        C("fail_reason_code", "failReasonCode", "VARCHAR", "String"),
        C("auto_next_flag", "autoNextFlag", "TINYINT", "Integer"),
        C("cost_cent", "costCent", "INTEGER", "Integer"),
        C("queued_time", "queuedTime", "TIMESTAMP", "Date"),
        C("started_time", "startedTime", "TIMESTAMP", "Date"),
        C("finished_time", "finishedTime", "TIMESTAMP", "Date"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_asset", "ShortAsset", [
        C("id", "id", "BIGINT", "Long"),
        C("asset_id", "assetId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("shot_id", "shotId", "VARCHAR", "String"),
        C("asset_type", "assetType", "VARCHAR", "String"),
        C("storage_key", "storageKey", "LONGVARCHAR", "String"),
        C("file_url", "fileUrl", "VARCHAR", "String"),
        C("file_size_byte", "fileSizeByte", "INTEGER", "Integer"),
        C("duration_sec", "durationSec", "INTEGER", "Integer"),
        C("width", "width", "INTEGER", "Integer"),
        C("height", "height", "INTEGER", "Integer"),
        C("source_task_id", "sourceTaskId", "VARCHAR", "String"),
        C("selected_flag", "selectedFlag", "TINYINT", "Integer"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_film", "ShortFilm", [
        C("id", "id", "BIGINT", "Long"),
        C("film_id", "filmId", "VARCHAR", "String"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("user_id", "userId", "VARCHAR", "String"),
        C("film_asset_id", "filmAssetId", "VARCHAR", "String"),
        C("film_duration_sec", "filmDurationSec", "INTEGER", "Integer"),
        C("compose_status", "composeStatus", "VARCHAR", "String"),
        C("expired_flag", "expiredFlag", "TINYINT", "Integer"),
        C("subtitle_stale_flag", "subtitleStaleFlag", "TINYINT", "Integer"),
        C("compose_task_id", "composeTaskId", "VARCHAR", "String"),
        C("composed_time", "composedTime", "TIMESTAMP", "Date"),
        C("export_json", "exportJson", "LONGVARCHAR", "String"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_review_record", "ShortReviewRecord", [
        C("id", "id", "BIGINT", "Long"),
        C("review_id", "reviewId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("creator_user_id", "creatorUserId", "VARCHAR", "String"),
        C("review_status", "reviewStatus", "VARCHAR", "String"),
        C("risk_level", "riskLevel", "VARCHAR", "String"),
        C("reviewer_user_id", "reviewerUserId", "VARCHAR", "String"),
        C("review_comment", "reviewComment", "VARCHAR", "String"),
        C("reject_item_json", "rejectItemJson", "LONGVARCHAR", "String"),
        C("submitted_time", "submittedTime", "TIMESTAMP", "Date"),
        C("reviewed_time", "reviewedTime", "TIMESTAMP", "Date"),
        C("deleted_flag", "deletedFlag", "TINYINT", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
        C("updated_time", "updatedTime", "TIMESTAMP", "Date"),
    ]),
    ("short_compliance_hit", "ShortComplianceHit", [
        C("id", "id", "BIGINT", "Long"),
        C("hit_id", "hitId", "VARCHAR", "String"),
        C("work_id", "workId", "VARCHAR", "String"),
        C("episode_id", "episodeId", "VARCHAR", "String"),
        C("shot_id", "shotId", "VARCHAR", "String"),
        C("check_scope", "checkScope", "VARCHAR", "String"),
        C("risk_level", "riskLevel", "VARCHAR", "String"),
        C("risk_category", "riskCategory", "VARCHAR", "String"),
        C("hit_text", "hitText", "VARCHAR", "String"),
        C("suggestion", "suggestion", "VARCHAR", "String"),
        C("scene_no", "sceneNo", "INTEGER", "Integer"),
        C("line_no", "lineNo", "INTEGER", "Integer"),
        C("shot_no", "shotNo", "INTEGER", "Integer"),
        C("created_time", "createdTime", "TIMESTAMP", "Date"),
    ]),
]


def cap(name: str) -> str:
    return name[0].upper() + name[1:]


def write_model(cls, cols):
    needs_date = any(c[3] == "Date" for c in cols)
    lines = ["package com.bgssai.shortai.model;", "", "import java.io.Serializable;"]
    if needs_date:
        lines.append("import java.util.Date;")
    lines += ["", f"public class {cls} implements Serializable {{", "    private static final long serialVersionUID = 1L;", ""]
    for db, java, jdbc, jtype in cols:
        lines.append(f"    private {jtype} {java};")
        lines.append("")
    for db, java, jdbc, jtype in cols:
        lines.append(f"    public {jtype} get{cap(java)}() {{")
        lines.append(f"        return {java};")
        lines.append("    }")
        lines.append("")
        lines.append(f"    public void set{cap(java)}({jtype} {java}) {{")
        if jtype == "String":
            lines.append(f"        this.{java} = {java} == null ? null : {java}.trim();")
        else:
            lines.append(f"        this.{java} = {java};")
        lines.append("    }")
        lines.append("")
    lines.append("}")
    return "\n".join(lines) + "\n"


CRITERION = r'''
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
            this.typeHandler = null;
            this.noValue = true;
        }
        protected Criterion(String condition, Object value, String typeHandler) {
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof java.util.List<?>) { this.listValue = true; } else { this.singleValue = true; }
        }
        protected Criterion(String condition, Object value) { this(condition, value, null); }
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
'''


def write_example(cls, cols):
    needs_date = any(c[3] == "Date" for c in cols)
    lines = [
        "package com.bgssai.shortai.model;",
        "",
        "import java.util.ArrayList;",
        "import java.util.List;",
    ]
    if needs_date:
        lines.append("import java.util.Date;")
    lines += [
        "",
        f"public class {cls}Example {{",
        "    protected String orderByClause;",
        "    protected boolean distinct;",
        "    protected List<Criteria> oredCriteria;",
        f"    public {cls}Example() {{ oredCriteria = new ArrayList<>(); }}",
        "    public void setOrderByClause(String orderByClause) { this.orderByClause = orderByClause; }",
        "    public String getOrderByClause() { return orderByClause; }",
        "    public void setDistinct(boolean distinct) { this.distinct = distinct; }",
        "    public boolean isDistinct() { return distinct; }",
        "    public List<Criteria> getOredCriteria() { return oredCriteria; }",
        "    public void or(Criteria criteria) { oredCriteria.add(criteria); }",
        "    public Criteria or() { Criteria criteria = createCriteriaInternal(); oredCriteria.add(criteria); return criteria; }",
        "    public Criteria createCriteria() { Criteria criteria = createCriteriaInternal(); if (oredCriteria.size() == 0) { oredCriteria.add(criteria); } return criteria; }",
        "    protected Criteria createCriteriaInternal() { return new Criteria(); }",
        "    public void clear() { oredCriteria.clear(); orderByClause = null; distinct = false; }",
        "    protected abstract static class GeneratedCriteria {",
        "        protected List<Criterion> criteria;",
        "        protected GeneratedCriteria() { criteria = new ArrayList<>(); }",
        "        public boolean isValid() { return criteria.size() > 0; }",
        "        public List<Criterion> getAllCriteria() { return criteria; }",
        "        public List<Criterion> getCriteria() { return criteria; }",
        '        protected void addCriterion(String condition) { if (condition == null) { throw new RuntimeException("Value for condition cannot be null"); } criteria.add(new Criterion(condition)); }',
        '        protected void addCriterion(String condition, Object value, String property) { if (value == null) { throw new RuntimeException("Value for " + property + " cannot be null"); } criteria.add(new Criterion(condition, value)); }',
        '        protected void addCriterion(String condition, Object value1, Object value2, String property) { if (value1 == null || value2 == null) { throw new RuntimeException("Between values for " + property + " cannot be null"); } criteria.add(new Criterion(condition, value1, value2)); }',
    ]
    for db, java, jdbc, jtype in cols:
        lines.append(f'        public Criteria and{cap(java)}IsNull() {{ addCriterion("{db} is null"); return (Criteria) this; }}')
        lines.append(f'        public Criteria and{cap(java)}IsNotNull() {{ addCriterion("{db} is not null"); return (Criteria) this; }}')
        lines.append(f'        public Criteria and{cap(java)}EqualTo({jtype} value) {{ addCriterion("{db} =", value, "{java}"); return (Criteria) this; }}')
        lines.append(f'        public Criteria and{cap(java)}NotEqualTo({jtype} value) {{ addCriterion("{db} <>", value, "{java}"); return (Criteria) this; }}')
        lines.append(f'        public Criteria and{cap(java)}GreaterThan({jtype} value) {{ addCriterion("{db} >", value, "{java}"); return (Criteria) this; }}')
        lines.append(f'        public Criteria and{cap(java)}LessThan({jtype} value) {{ addCriterion("{db} <", value, "{java}"); return (Criteria) this; }}')
        if jtype == "String":
            lines.append(f'        public Criteria and{cap(java)}Like(String value) {{ addCriterion("{db} like", value, "{java}"); return (Criteria) this; }}')
        lines.append(f'        public Criteria and{cap(java)}In(List<{jtype}> values) {{ addCriterion("{db} in", values, "{java}"); return (Criteria) this; }}')
    lines.append("    }")
    lines.append(CRITERION)
    return "\n".join(lines) + "\n"


def write_mapper(cls):
    return f"""package com.bgssai.shortai.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bgssai.shortai.model.{cls};
import com.bgssai.shortai.model.{cls}Example;

@Mapper
public interface {cls}Mapper {{
    long countByExample({cls}Example example);
    int insert({cls} row);
    int insertSelective({cls} row);
    List<{cls}> selectByExample({cls}Example example);
    {cls} selectOneByExample({cls}Example example);
    {cls} selectByPrimaryKey(Long id);
    int updateByExampleSelective(@Param("row") {cls} row, @Param("example") {cls}Example example);
    int updateByPrimaryKeySelective({cls} row);
}}
"""


WHERE = r'''  <sql id="Example_Where_Clause">
    <where>
      <foreach collection="oredCriteria" item="criteria" separator="or">
        <if test="criteria.valid">
          <trim prefix="(" prefixOverrides="and" suffix=")">
            <foreach collection="criteria.criteria" item="criterion">
              <choose>
                <when test="criterion.noValue">and ${criterion.condition}</when>
                <when test="criterion.singleValue">and ${criterion.condition} #{criterion.value}</when>
                <when test="criterion.betweenValue">and ${criterion.condition} #{criterion.value} and #{criterion.secondValue}</when>
                <when test="criterion.listValue">
                  and ${criterion.condition}
                  <foreach close=")" collection="criterion.value" item="listItem" open="(" separator=",">#{listItem}</foreach>
                </when>
              </choose>
            </foreach>
          </trim>
        </if>
      </foreach>
    </where>
  </sql>
  <sql id="Update_By_Example_Where_Clause">
    <where>
      <foreach collection="example.oredCriteria" item="criteria" separator="or">
        <if test="criteria.valid">
          <trim prefix="(" prefixOverrides="and" suffix=")">
            <foreach collection="criteria.criteria" item="criterion">
              <choose>
                <when test="criterion.noValue">and ${criterion.condition}</when>
                <when test="criterion.singleValue">and ${criterion.condition} #{criterion.value}</when>
                <when test="criterion.betweenValue">and ${criterion.condition} #{criterion.value} and #{criterion.secondValue}</when>
                <when test="criterion.listValue">
                  and ${criterion.condition}
                  <foreach close=")" collection="criterion.value" item="listItem" open="(" separator=",">#{listItem}</foreach>
                </when>
              </choose>
            </foreach>
          </trim>
        </if>
      </foreach>
    </where>
  </sql>
'''


def write_xml(table, cls, cols):
    result = []
    for db, java, jdbc, jtype in cols:
        tag = "id" if db == "id" else "result"
        result.append(f'    <{tag} column="{db}" jdbcType="{jdbc}" property="{java}" />')
    col_list = ", ".join(c[0] for c in cols)
    insert_cols = []
    insert_vals = []
    sel_ifs = []
    upd_example = []
    upd_pk = []
    for db, java, jdbc, jtype in cols:
        insert_cols.append(f'      <if test="{java} != null">{db},</if>')
        insert_vals.append(f'      <if test="{java} != null">#{{{java},jdbcType={jdbc}}},</if>')
        sel_ifs.append(f'      <if test="{java} != null">{db},</if>')
        if db != "id":
            upd_example.append(f'      <if test="row.{java} != null">{db} = #{{row.{java},jdbcType={jdbc}}},</if>')
            upd_pk.append(f'      <if test="{java} != null">{db} = #{{{java},jdbcType={jdbc}}},</if>')
    full_insert_cols = ",\n      ".join(c[0] for c in cols)
    full_insert_vals = ",\n      ".join(f"#{{{c[1]},jdbcType={c[2]}}}" for c in cols)
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.bgssai.shortai.mapper.{cls}Mapper">
  <resultMap id="BaseResultMap" type="com.bgssai.shortai.model.{cls}">
{chr(10).join(result)}
  </resultMap>
{WHERE}
  <sql id="Base_Column_List">
    {col_list}
  </sql>
  <select id="selectByExample" parameterType="com.bgssai.shortai.model.{cls}Example" resultMap="BaseResultMap">
    select
    <if test="distinct">distinct</if>
    <include refid="Base_Column_List" />
    from {table}
    <if test="_parameter != null"><include refid="Example_Where_Clause" /></if>
    <if test="orderByClause != null">order by ${{orderByClause}}</if>
  </select>
  <select id="selectOneByExample" parameterType="com.bgssai.shortai.model.{cls}Example" resultMap="BaseResultMap">
    select
    <include refid="Base_Column_List" />
    from {table}
    <if test="_parameter != null"><include refid="Example_Where_Clause" /></if>
    <if test="orderByClause != null">order by ${{orderByClause}}</if>
    limit 1
  </select>
  <select id="selectByPrimaryKey" parameterType="java.lang.Long" resultMap="BaseResultMap">
    select <include refid="Base_Column_List" /> from {table} where id = #{{id,jdbcType=BIGINT}}
  </select>
  <select id="countByExample" parameterType="com.bgssai.shortai.model.{cls}Example" resultType="java.lang.Long">
    select count(*) from {table}
    <if test="_parameter != null"><include refid="Example_Where_Clause" /></if>
  </select>
  <insert id="insert" parameterType="com.bgssai.shortai.model.{cls}" useGeneratedKeys="true" keyProperty="id">
    insert into {table} (
      {full_insert_cols}
    ) values (
      {full_insert_vals}
    )
  </insert>
  <insert id="insertSelective" parameterType="com.bgssai.shortai.model.{cls}" useGeneratedKeys="true" keyProperty="id">
    insert into {table}
    <trim prefix="(" suffix=")" suffixOverrides=",">
{chr(10).join(insert_cols)}
    </trim>
    <trim prefix="values (" suffix=")" suffixOverrides=",">
{chr(10).join(insert_vals)}
    </trim>
  </insert>
  <update id="updateByExampleSelective">
    update {table}
    <set>
{chr(10).join(upd_example)}
    </set>
    <if test="example != null"><include refid="Update_By_Example_Where_Clause" /></if>
  </update>
  <update id="updateByPrimaryKeySelective" parameterType="com.bgssai.shortai.model.{cls}">
    update {table}
    <set>
{chr(10).join(upd_pk)}
    </set>
    where id = #{{id,jdbcType=BIGINT}}
  </update>
</mapper>
'''


def main():
    for table, cls, cols in TABLES:
        model = write_model(cls, cols)
        example = write_example(cls, cols)
        mapper = write_mapper(cls)
        xml = write_xml(table, cls, cols)
        for java_root, xml_root in ((USER_JAVA, USER_XML), (ADMIN_JAVA, ADMIN_XML)):
            (java_root / "model").mkdir(parents=True, exist_ok=True)
            (java_root / "mapper").mkdir(parents=True, exist_ok=True)
            xml_root.mkdir(parents=True, exist_ok=True)
            (java_root / "model" / f"{cls}.java").write_text(model, encoding="utf-8")
            (java_root / "model" / f"{cls}Example.java").write_text(example, encoding="utf-8")
            (java_root / "mapper" / f"{cls}Mapper.java").write_text(mapper, encoding="utf-8")
            (xml_root / f"{cls}Mapper.xml").write_text(xml, encoding="utf-8")
        print("generated", cls)


if __name__ == "__main__":
    main()
