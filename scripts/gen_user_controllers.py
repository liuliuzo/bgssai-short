from pathlib import Path

ROOT = Path(r"C:\Users\lzhao3730\Desktop\github\bgssai-short\bgssai-short-user\bgssai-short-user\src\main\java\com\bgssai\shortai\production\ui")
ROOT.mkdir(parents=True, exist_ok=True)

CTRL = '''package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.{method_ann};
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.common.ratelimit.RateLimit;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.{svc};

@RestController
public class {cls} {{

    @Autowired
    private {svc} service;

{body}
}}
'''

# (class, service, body)
items = []

def post(cls, svc, path, call, req=None, rate=False):
    anns = '    @NeedAop\n'
    if rate:
        anns += '    @RateLimit(capacity = 20, refillPerMinute = 20, keyBy = RateLimit.Key.USER)\n'
    anns += f'    @PostMapping("{path}")\n'
    if req:
        method = f'''{anns}    public ApiResponse handle(@RequestBody ProductionDtos.{req} req) {{
        return service.{call}(req);
    }}'''
    else:
        method = f'''{anns}    public ApiResponse handle() {{
        return service.{call}();
    }}'''
    items.append((cls, svc, method, "PostMapping"))

def get(cls, svc, path, call, params, ret="ApiResponse"):
    args = []
    pass_args = []
    for name, typ, default in params:
        if default is None:
            args.append(f'@RequestParam(value = "{name}", required = false) {typ} {name.replace("-", "_")}')
            pass_args.append(name.replace("-", "_"))
        else:
            args.append(f'@RequestParam(value = "{name}", required = false, defaultValue = "{default}") {typ} {name.replace("-", "_")}')
            pass_args.append(name.replace("-", "_"))
    sig = ", ".join(args)
    call_sig = ", ".join(pass_args)
    method = f'''    @NeedAop
    @GetMapping("{path}")
    public {ret} handle({sig}) {{
        return service.{call}({call_sig});
    }}'''
    items.append((cls, svc, method, "GetMapping"))

post("CreateShortWorkController", "WorkService", "/bgssai/user/short-works/create", "create", "WorkCreateReq")
post("UpdateShortWorkController", "WorkService", "/bgssai/user/short-works/update", "update", "WorkUpdateReq")
post("ArchiveShortWorkController", "WorkService", "/bgssai/user/short-works/archive", "archive", "WorkArchiveReq")
post("CreateEpisodeController", "WorkService", "/bgssai/user/episodes/create", "createEpisode", "EpisodeReq")
post("UpdateEpisodeController", "WorkService", "/bgssai/user/episodes/update", "updateEpisode", "EpisodeReq")
post("DeleteEpisodeController", "WorkService", "/bgssai/user/episodes/delete", "deleteEpisode", "EpisodeReq")
post("BatchEpisodeController", "WorkService", "/bgssai/user/episodes/batch", "batchNotImplemented")
get("ListShortWorksController", "WorkService", "/bgssai/user/short-works/list", "list",
    [("page_num", "int", "1"), ("page_size", "int", "20"), ("work_status", "String", None),
     ("genre", "String", None), ("keyword", "String", None)], "PageResponse")
get("GetShortWorkDetailController", "WorkService", "/bgssai/user/short-works/detail", "detail",
    [("work_id", "String", None)])
get("ListQualityTemplatesController", "WorkService", "/bgssai/user/quality/templates", "emptyTemplates",
    [("genre", "String", None)])

post("GenerateOutlineController", "ScriptService", "/bgssai/user/scripts/outline-generate", "generateOutline", "ScriptGenerateReq")
post("SaveOutlineController", "ScriptService", "/bgssai/user/scripts/outline-save", "saveOutline", "OutlineSaveReq")
post("GenerateScriptController", "ScriptService", "/bgssai/user/scripts/generate", "generateScript", "ScriptGenerateReq")
post("SaveScriptController", "ScriptService", "/bgssai/user/scripts/save", "save", "ScriptSaveReq")
post("RewriteScriptController", "ScriptService", "/bgssai/user/scripts/rewrite", "rewrite", "ScriptGenerateReq")
post("ReadyScriptController", "ScriptService", "/bgssai/user/scripts/ready", "ready", "ScriptGenerateReq")
post("RollbackScriptController", "ScriptService", "/bgssai/user/scripts/rollback", "rollback", "ScriptGenerateReq")
get("GetOutlineController", "ScriptService", "/bgssai/user/scripts/outline", "getOutline", [("work_id", "String", None)])
get("GetScriptDetailController", "ScriptService", "/bgssai/user/scripts/detail", "detail", [("episode_id", "String", None)])
get("ListScriptVersionsController", "ScriptService", "/bgssai/user/scripts/versions", "versions", [("episode_id", "String", None)])

get("ListCharactersController", "CharacterService", "/bgssai/user/characters/list", "listCharacters", [("work_id", "String", None)])
post("CreateCharacterController", "CharacterService", "/bgssai/user/characters/create", "createCharacter", "CharacterReq")
post("UpdateCharacterController", "CharacterService", "/bgssai/user/characters/update", "updateCharacter", "CharacterReq")
post("DeleteCharacterController", "CharacterService", "/bgssai/user/characters/delete", "deleteCharacter", "CharacterReq")
post("PortraitCharacterController", "CharacterService", "/bgssai/user/characters/portrait-generate", "portrait", "CharacterReq")
post("LockCharacterController", "CharacterService", "/bgssai/user/characters/lock", "lockCharacter", "CharacterReq")
get("ListSceneCardsController", "CharacterService", "/bgssai/user/scene-cards/list", "listScenes", [("work_id", "String", None)])
post("CreateSceneCardController", "CharacterService", "/bgssai/user/scene-cards/create", "createScene", "CharacterReq")
post("LockSceneCardController", "CharacterService", "/bgssai/user/scene-cards/lock", "lockScene", "CharacterReq")
post("PortraitSceneCardController", "CharacterService", "/bgssai/user/scene-cards/portrait-generate", "portraitScene", "CharacterReq")

post("SplitShotsController", "ShotService", "/bgssai/user/shots/split-from-script", "split", "ShotReq")
post("UpdateShotController", "ShotService", "/bgssai/user/shots/update", "update", "ShotReq")
get("ListShotsController", "ShotService", "/bgssai/user/shots/list", "list",
    [("episode_id", "String", None), ("page_num", "int", "1"), ("page_size", "int", "50"), ("shot_status", "String", None)],
    "PageResponse")

post("EstimateAiTasksController", "AiTaskService", "/bgssai/user/ai-tasks/estimate", "estimate", "AiTaskReq")
post("SubmitAiTasksController", "AiTaskService", "/bgssai/user/ai-tasks/submit", "submit", "AiTaskReq", True)
post("RetryAiTaskController", "AiTaskService", "/bgssai/user/ai-tasks/retry", "retry", "AiTaskReq", True)
post("CancelAiTaskController", "AiTaskService", "/bgssai/user/ai-tasks/cancel", "cancel", "AiTaskReq")
get("ListAiTasksController", "AiTaskService", "/bgssai/user/ai-tasks/list", "list",
    [("page_num", "int", "1"), ("page_size", "int", "20"), ("task_type", "String", None), ("task_status", "String", None),
     ("batch_id", "String", None), ("episode_id", "String", None), ("work_id", "String", None)], "PageResponse")
get("BatchProgressController", "AiTaskService", "/bgssai/user/ai-tasks/batch-progress", "batchProgress",
    [("batch_id", "String", None)])

get("FilmPrecheckController", "FilmService", "/bgssai/user/films/precheck", "precheck", [("episode_id", "String", None)])
get("FilmTimelineController", "FilmService", "/bgssai/user/films/timeline", "timeline", [("episode_id", "String", None)])
get("FilmDetailController", "FilmService", "/bgssai/user/films/detail", "detail", [("episode_id", "String", None)])
get("FilmDownloadUrlController", "FilmService", "/bgssai/user/films/download-url", "downloadUrl", [("asset_id", "String", None)])
post("ComposeFilmController", "FilmService", "/bgssai/user/films/compose", "compose", "FilmReq")

post("SubmitReviewController", "ReviewUserService", "/bgssai/user/reviews/submit", "submit", "ReviewUserReq")
post("WithdrawReviewController", "ReviewUserService", "/bgssai/user/reviews/withdraw", "withdraw", "ReviewUserReq")
get("ListUserReviewsController", "ReviewUserService", "/bgssai/user/reviews/list", "list", [("work_id", "String", None)])
post("PrecheckController", "ComplianceService", "/bgssai/user/compliance/precheck", "precheck", "PrecheckReq")

for cls, svc, body, method_ann in items:
    imports = "GetMapping" if method_ann == "GetMapping" else "PostMapping"
    unused = []
    text = CTRL.format(method_ann=imports, svc=svc, cls=cls, body=body)
    if "RequestParam" not in body:
        text = text.replace("import org.springframework.web.bind.annotation.RequestParam;\n", "")
    if "RequestBody" not in body:
        text = text.replace("import org.springframework.web.bind.annotation.RequestBody;\n", "")
    if "PageResponse" not in body:
        text = text.replace("import com.bgssai.shortai.common.PageResponse;\n", "")
    if "RateLimit" not in body:
        text = text.replace("import com.bgssai.shortai.common.ratelimit.RateLimit;\n", "")
    if "ProductionDtos" not in body:
        text = text.replace("import com.bgssai.shortai.production.dto.ProductionDtos;\n", "")
    (ROOT / f"{cls}.java").write_text(text, encoding="utf-8")
    print(cls)

# lock/unlock special
lock = '''package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.ShotService;

@RestController
public class LockShotController {

    @Autowired
    private ShotService service;

    @NeedAop
    @PostMapping("/bgssai/user/shots/lock")
    public ApiResponse handle(@RequestBody ProductionDtos.ShotReq req) {
        return service.lock(req, true);
    }
}
'''
unlock = lock.replace("LockShotController", "UnlockShotController").replace("/lock", "/unlock").replace("true", "false")
(ROOT / "LockShotController.java").write_text(lock, encoding="utf-8")
(ROOT / "UnlockShotController.java").write_text(unlock, encoding="utf-8")

asset = '''package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.production.service.AssetService;

@RestController
public class GetAssetController {

    @Autowired
    private AssetService assetService;

    @GetMapping("/bgssai/user/assets/{asset_id}")
    public ResponseEntity<byte[]> handle(@PathVariable("asset_id") String assetId) {
        return assetService.get(assetId);
    }
}
'''
(ROOT / "GetAssetController.java").write_text(asset, encoding="utf-8")
print("done", len(list(ROOT.glob("*.java"))))
