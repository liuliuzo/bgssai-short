package com.bgssai.shortai.mcp.service;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Complete JSON schemas, including nested array items, for production clients. */
final class McpProductionSchemas {
    private static final ObjectMapper JSON = new ObjectMapper();
    private McpProductionSchemas() { }

    static ArrayNode definitions() {
        ArrayNode tools = JSON.createArrayNode();
        tools.add(tool("list_models", "只读正式用户模型目录。返回公开模型编码、能力及价格；目录列出已启用模型，不代表实时网关成功。未返回的时长限制不可推断。",
                schema(List.of(), choice("ability_type", "可选能力", "TEXT", "IMAGE", "VIDEO", "VOICE", "MUSIC", "COMPOSE"))));
        tools.add(tool("ready_episode_script", "将已保存的剧本设为 READY，沿用节奏与合规检查并抽取角色/场景卡。force_ready=true 仅显式接受低节奏评分，不绕过合规。",
                schema(List.of("episode_id"), text("episode_id", "分集 ID"), prop("force_ready", "boolean", "默认 false"))));
        tools.add(tool("split_storyboard", "从 READY 剧本逐行创建分镜；会替换未锁定分镜，须 confirm_replace=true。随后用 list_shots 获取 shot_id 并用 save_storyboard 编辑。",
                schema(List.of("episode_id", "confirm_replace"), text("episode_id", "分集 ID"), prop("confirm_replace", "boolean", "必须为 true"))));
        ObjectNode shot = schema(List.of("shot_id"), text("shot_id", "已拆分镜头 ID"),
                text("shot_desc", "画面人物、地点与动作"), text("dialogue_text", "配音台词；空串清除"),
                text("shot_size", "景别编码，例如 MEDIUM/CLOSE/WIDE"), text("camera_move", "运镜编码，例如 FIXED/PUSH"),
                integer("shot_duration_sec", "镜头秒数；还须符合所选模型的真实能力", 1, 120), text("extra_prompt", "额外画面约束，正式服务重算 image_prompt"));
        tools.add(tool("save_storyboard", "原子保存同一分集的结构化分镜修改，锁定镜头不可改；不覆盖生成资产或直接写模型提示词。",
                schema(List.of("episode_id", "shots"), text("episode_id", "分集 ID"), array("shots", "1-100 个镜头", shot, 100))));
        tools.add(tool("get_render_task", "查询本人任务真实状态、进度、公开错误、费用和 result_json；不触发任务或重试。",
                schema(List.of("task_id"), text("task_id", "任务 ID"))));
        tools.add(tool("get_episode_film", "读取本人分集成片、真实可播放 download_url、过期状态、合成预检、配音和字幕；未完成时不提供占位成片。",
                schema(List.of("episode_id"), text("episode_id", "分集 ID"))));
        tools.add(tool("save_film_settings", "保存作品共享的合成设置，校验本集背景音乐资产。影响同作品后续合成，不自动生成或重合成。",
                schema(List.of("episode_id"), text("episode_id", "分集 ID"), text("bgm_asset_id", "get_episode_film 返回的本集音乐资产；空串取消"),
                        prop("burn_subtitle", "boolean", "是否烧录正式字幕轨"), choice("aspect_ratio", "画幅", "9:16", "16:9", "1:1"))));
        tools.add(tool("list_reference_cards", "列出本人作品的角色、场景、道具卡及真实参考图候选、锁定状态。",
                schema(List.of("work_id"), text("work_id", "作品 ID"))));
        tools.add(tool("generate_reference_image", "为已有角色/场景/道具卡调用正式参考图任务，需 confirm_cost=true；完成后查询候选图并显式选择锁定。",
                schema(List.of("work_id", "card_type", "card_id", "confirm_cost"), text("work_id", "作品 ID"),
                        choice("card_type", "卡类型", "CHARACTER", "SCENE", "PROP"), text("card_id", "角色或场景卡 ID"),
                        text("model_code", "list_models 的 IMAGE 编码；省略使用正式默认模型"),
                        text("appearance_prompt", "仅 CHARACTER：先通过正式角色编辑保存年龄、服饰等外观，再生成参考图；影响已绑定镜头"),
                        integer("candidate_count", "仅 2/4/8，默认 2", 2, 8), prop("confirm_cost", "boolean", "必须为 true"))));
        tools.add(tool("lock_reference_card", "将该卡已生成的真实候选图设为定妆参考，沿用正式锁定和镜头过期规则。",
                schema(List.of("work_id", "card_type", "card_id", "asset_id"), text("work_id", "作品 ID"),
                        choice("card_type", "卡类型", "CHARACTER", "SCENE", "PROP"), text("card_id", "角色或场景卡 ID"), text("asset_id", "该卡 candidate_list 中的资产 ID"))));
        tools.add(tool("align_dubbing", "按正式配音服务处理时长差异，不生成收费任务。EXTEND_SHOT 会改变镜头时长，需重新检查视频及成片时长。",
                schema(List.of("episode_id", "shot_id", "align_action"), text("episode_id", "分集 ID"), text("shot_id", "分镜 ID"),
                        choice("align_action", "对齐动作", "PAD_SILENCE", "ADJUST_SPEED", "EXTEND_SHOT"))));
        tools.add(tool("list_voices", "列出正式产品声音目录；配音生成仍由所选模型与供应商适配层执行。",
                schema(List.of())));
        tools.add(tool("set_character_voice", "为本人角色设置正式目录中的 voice_id，供后续 VOICE 任务使用；不自动重生成或收费。",
                schema(List.of("character_id", "voice_id"), text("character_id", "角色 ID"), text("voice_id", "list_voices 返回的声音 ID"))));
        return tools;
    }

    static ObjectNode renderSchema() {
        return schema(List.of("episode_id", "confirm_cost"), text("episode_id", "分集 ID"),
                choice("task_type", "默认 COMPOSE；MUSIC 需 prompt；COMPOSE 不接受 shot_id/model_code", "IMAGE", "VIDEO", "VOICE", "MUSIC", "COMPOSE"),
                text("shot_id", "IMAGE/VIDEO/VOICE 可指定一个镜头；省略操作本集适用镜头"),
                text("model_code", "list_models 公开编码；省略使用正式默认模型"),
                text("prompt", "仅 MUSIC：背景音乐描述；IMAGE/VIDEO 使用已保存的分镜画面；VOICE 使用台词"),
                text("lyrics", "仅 MUSIC：可选歌词"), prop("is_instrumental", "boolean", "仅 MUSIC，默认 true"),
                prop("confirm_cost", "boolean", "必须显式为 true；每次调用都是新任务，请勿自动重试"));
    }

    static ObjectNode sceneList() {
        ObjectNode line = schema(List.of("line_no", "character_name", "line_text", "estimated_sec"),
                integer("line_no", "台词序号", 1, 1000), text("character_name", "角色名"), text("line_text", "台词或旁白"),
                text("emotion", "情绪编码"), integer("estimated_sec", "该行镜头估计秒数", 1, 120));
        ObjectNode scene = schema(List.of("scene_no", "scene_location", "scene_summary", "line_list"),
                integer("scene_no", "场次序号", 1, 1000), text("scene_location", "地点"), text("scene_time", "时间，例如 NIGHT"),
                text("scene_summary", "场次画面与动作"), array("line_list", "逐行拆成镜头", line, 100));
        ObjectNode value = array("scene_list", "结构化场次；每行对应正式拆分的一个镜头", scene, 100);
        return value;
    }

    private static ObjectNode tool(String name, String description, ObjectNode input) {
        ObjectNode node = JSON.createObjectNode().put("name", name).put("description", description);
        node.set("inputSchema", input);
        return node;
    }
    private static ObjectNode schema(List<String> required, ObjectNode... properties) {
        ObjectNode node = JSON.createObjectNode().put("type", "object").put("additionalProperties", false);
        ObjectNode props = node.putObject("properties");
        for (ObjectNode property : properties) props.set(property.remove("__name").asText(), property);
        ArrayNode req = node.putArray("required");
        required.forEach(req::add);
        return node;
    }
    private static ObjectNode prop(String name, String type, String description) {
        return JSON.createObjectNode().put("__name", name).put("type", type).put("description", description);
    }
    private static ObjectNode text(String name, String description) { return prop(name, "string", description); }
    private static ObjectNode integer(String name, String description, int min, int max) {
        return prop(name, "integer", description).put("minimum", min).put("maximum", max);
    }
    private static ObjectNode choice(String name, String description, String... values) {
        ObjectNode node = text(name, description);
        ArrayNode choices = node.putArray("enum");
        for (String value : values) choices.add(value);
        return node;
    }
    private static ObjectNode array(String name, String description, ObjectNode items, int max) {
        ObjectNode node = prop(name, "array", description).put("minItems", 1).put("maxItems", max);
        node.set("items", items);
        return node;
    }
}
