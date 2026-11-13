package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;
import com.bgssai.shortai.production.support.ImageToVideoPolicy;
import com.bgssai.shortai.production.support.OfficialVideoCopy;
import com.bgssai.shortai.production.support.OfficialVideoLane;
import com.bgssai.shortai.production.support.ShotConsistency;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.production.support.TaskPrices;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.quota.service.QuotaWalletService;

/**
 * Official shot video. A real PNG or JPEG (first frame, or a locked look / scene / prop)
 * goes to CPU image-to-video. Anything else, including a seed SVG, stays on text-to-video.
 * Neither path resumes an old receipt, and neither deletes the current clip before success.
 */
@Service
public class OfficialShotVideoService {

    @Autowired
    private AiTaskService aiTaskService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private ImageToVideoService imageToVideoService;
    @Autowired
    private AiModelResolveService aiModelResolveService;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private QuotaWalletService quotaWalletService;
    @Autowired
    private QuotaConsumeService quotaConsumeService;
    @Autowired
    private PaidOrderGrantService paidOrderGrantService;
    @Autowired
    private WorkAccessService workAccessService;

    public ApiResponse preview(AiTaskReq req) {
        String userId = workAccessService.requireUser();
        paidOrderGrantService.ensureFreeTier(userId);
        List<Plan> plans = planAll(req);
        int cost = 0;
        for (Plan plan : plans) {
            if (!plan.blocked) {
                cost += plan.costCent;
            }
        }
        quotaConsumeService.assertAffordable(userId, cost);
        Map<String, Object> result = wallet(userId);
        result.put("task_count", plans.size());
        result.put("estimated_cost_cent", cost);
        result.put("shot_list", views(plans));
        result.put("resumed_existing_video", false);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse submit(AiTaskReq req) {
        if (req == null || !Integer.valueOf(1).equals(req.getConfirmCost())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, OfficialVideoCopy.CONFIRM);
        }
        String userId = workAccessService.requireUser();
        paidOrderGrantService.ensureFreeTier(userId);
        List<Plan> plans = planAll(req);
        List<Plan> runnable = new ArrayList<>();
        int cost = 0;
        for (Plan plan : plans) {
            if (!plan.blocked) {
                runnable.add(plan);
                cost += plan.costCent;
            }
        }
        if (runnable.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, plans.isEmpty()
                    ? "没有可生成的镜头" : plans.get(0).blockReason);
        }
        quotaConsumeService.assertAffordable(userId, cost);
        String batchId = ShortIds.id("b-");
        List<String> ids = new ArrayList<>();
        List<Map<String, Object>> routes = new ArrayList<>();
        for (Plan plan : runnable) {
            scan(plan.shot);
            ShortAiTask task = enqueue(plan, batchId);
            ids.add(task.getTaskId());
            markGenerating(plan.shot);
            Map<String, Object> route = view(plan);
            route.put("task_id", task.getTaskId());
            routes.add(route);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_id", batchId);
        result.put("task_id_list", ids);
        result.put("task_count", ids.size());
        result.put("estimated_cost_cent", cost);
        result.put("shot_list", routes);
        result.put("resumed_existing_video", false);
        return Responses.ok(result);
    }

    private ShortAiTask enqueue(Plan plan, String batchId) {
        if (OfficialVideoCopy.ROUTE_I2V.equals(plan.route)) {
            return imageToVideoService.enqueueForShot(plan.shot, plan.assetId, plan.prompt, plan.costCent, batchId);
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("shot_id", plan.shot.getShotId());
        request.put("duration_sec", plan.shot.getShotDurationSec() == null ? 3 : plan.shot.getShotDurationSec());
        request.put("model_code", OfficialVideoLane.MODEL);
        return aiTaskQueue.enqueue(plan.shot.getUserId(), plan.shot.getWorkId(), plan.shot.getEpisodeId(),
                "VIDEO", "SHOT", plan.shot.getShotId(), request, batchId, 0, OfficialVideoLane.MODEL);
    }

    private void markGenerating(ShortShot shot) {
        ShortShot patch = new ShortShot();
        patch.setShotStatus("VIDEO_GENERATING");
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
    }

    private List<Plan> planAll(AiTaskReq req) {
        List<ShortShot> shots = aiTaskService.resolveShots(singleOrMany(req));
        boolean i2vReady = imageToVideoService != null && imageToVideoService.readyForOfficial();
        List<Plan> plans = new ArrayList<>();
        for (ShortShot shot : shots) {
            plans.add(plan(shot, i2vReady));
        }
        return plans;
    }

    private Plan plan(ShortShot shot, boolean i2vReady) {
        Plan plan = new Plan();
        plan.shot = shot;
        if ("LOCKED".equals(shot.getShotStatus())) {
            return blocked(plan, "镜" + shot.getShotNo() + " 已定稿，不能再生成");
        }
        if (videoBusy(shot)) {
            return blocked(plan, OfficialVideoCopy.IN_PROGRESS);
        }
        String assetId = realPhoto(shot);
        boolean useImage = assetId != null && i2vReady;
        try {
            aiTaskService.assertShotReady(shot, "VIDEO", useImage);
        } catch (BizException ex) {
            return blocked(plan, ex.getMessage());
        }
        if (useImage) {
            plan.route = OfficialVideoCopy.ROUTE_I2V;
            plan.routeName = "图生视频";
            plan.sizeNote = OfficialVideoCopy.I2V_SIZE;
            plan.assetId = assetId;
            plan.prompt = prompt(shot);
            plan.costCent = i2vCost();
            return plan;
        }
        plan.route = OfficialVideoCopy.ROUTE_T2V;
        plan.routeName = "文生视频";
        plan.sizeNote = assetId == null ? OfficialVideoCopy.T2V_NO_PHOTO : OfficialVideoCopy.T2V_UNAVAILABLE;
        plan.costCent = t2vCost(shot);
        return plan;
    }

    private static Plan blocked(Plan plan, String reason) {
        plan.blocked = true;
        plan.blockReason = reason;
        plan.route = "";
        plan.routeName = "";
        plan.sizeNote = reason;
        plan.costCent = 0;
        return plan;
    }

    private boolean videoBusy(ShortShot shot) {
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andUserIdEqualTo(shot.getUserId()).andWorkIdEqualTo(shot.getWorkId())
                .andTaskTypeEqualTo("VIDEO").andTaskStatusIn(List.of("QUEUED", "RUNNING", "PAUSED"));
        List<ShortAiTask> rows = shortAiTaskMapper.selectByExample(ex);
        if (rows == null) {
            return false;
        }
        for (ShortAiTask task : rows) {
            if (shot.getShotId().equals(task.getTargetId())) {
                return true;
            }
            if (ImageToVideoPolicy.TARGET.equals(task.getTargetType())
                    && shot.getShotId().equals(ShortJson.text(ShortJson.map(task.getRequestJson()).get("shot_id")))) {
                return true;
            }
        }
        return false;
    }

    private String realPhoto(ShortShot shot) {
        String first = usable(shot.getFirstFrameAssetId());
        if (first != null) {
            return first;
        }
        for (String characterId : ShotConsistency.characterIds(shot.getCharacterRefJson())) {
            ShortCharacterExample cx = new ShortCharacterExample();
            cx.createCriteria().andCharacterIdEqualTo(characterId).andDeletedFlagEqualTo(0);
            ShortCharacter row = shortCharacterMapper.selectOneByExample(cx);
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                String photo = usable(row.getRefImageAssetId());
                if (photo != null) {
                    return photo;
                }
            }
        }
        if (shot.getSceneCardId() != null && !shot.getSceneCardId().isBlank()) {
            String photo = lockedCardPhoto(shot.getSceneCardId());
            if (photo != null) {
                return photo;
            }
        }
        for (String propId : ShotConsistency.propIds(shot.getPropRefJson())) {
            String photo = lockedCardPhoto(propId);
            if (photo != null) {
                return photo;
            }
        }
        return null;
    }

    private String lockedCardPhoto(String cardId) {
        ShortSceneCardExample sx = new ShortSceneCardExample();
        sx.createCriteria().andSceneCardIdEqualTo(cardId).andDeletedFlagEqualTo(0);
        ShortSceneCard card = shortSceneCardMapper.selectOneByExample(sx);
        if (card == null || !"LOCKED".equals(card.getCardStatus())) {
            return null;
        }
        return usable(card.getRefImageAssetId());
    }

    private String usable(String assetId) {
        if (assetId == null || assetId.isBlank() || shortAssetMapper == null) {
            return null;
        }
        ShortAssetExample ax = new ShortAssetExample();
        ax.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ax);
        if (asset == null) {
            return null;
        }
        if (!OfficialVideoCopy.pngOrJpeg(asset.getStorageKey(), asset.getMimeType())) {
            return null;
        }
        if (asset.getFileSizeByte() != null && asset.getFileSizeByte() <= 0) {
            return null;
        }
        return asset.getAssetId();
    }

    private String prompt(ShortShot shot) {
        String base = shot.getImagePrompt() == null ? "" : shot.getImagePrompt().trim();
        if (base.isBlank()) {
            base = shot.getShotDesc() == null ? "" : shot.getShotDesc().trim();
        }
        String text = ShotConsistency.leadWithAnchors(base, anchorText(shot));
        if (text.length() > 2000) {
            return text.substring(0, 2000);
        }
        return text.isBlank() ? "竖屏短剧镜头，人物自然动作" : text;
    }

    private String anchorText(ShortShot shot) {
        List<ShortCharacter> characters = new ArrayList<>();
        for (String characterId : ShotConsistency.characterIds(shot.getCharacterRefJson())) {
            ShortCharacterExample cx = new ShortCharacterExample();
            cx.createCriteria().andCharacterIdEqualTo(characterId).andDeletedFlagEqualTo(0);
            ShortCharacter row = shortCharacterMapper.selectOneByExample(cx);
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                characters.add(row);
            }
        }
        ShortSceneCard scene = null;
        if (shot.getSceneCardId() != null && !shot.getSceneCardId().isBlank()) {
            ShortSceneCardExample sx = new ShortSceneCardExample();
            sx.createCriteria().andSceneCardIdEqualTo(shot.getSceneCardId()).andDeletedFlagEqualTo(0);
            ShortSceneCard row = shortSceneCardMapper.selectOneByExample(sx);
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                scene = row;
            }
        }
        List<ShortSceneCard> props = new ArrayList<>();
        for (String propId : ShotConsistency.propIds(shot.getPropRefJson())) {
            ShortSceneCardExample px = new ShortSceneCardExample();
            px.createCriteria().andSceneCardIdEqualTo(propId).andDeletedFlagEqualTo(0);
            ShortSceneCard row = shortSceneCardMapper.selectOneByExample(px);
            if (row != null && "LOCKED".equals(row.getCardStatus())) {
                props.add(row);
            }
        }
        return ShotConsistency.appearanceFragment(characters, scene, props);
    }

    private int i2vCost() {
        int seconds = (25 + 8 - 1) / 8;
        return Math.max(1, seconds) * officialUnit();
    }

    private int t2vCost(ShortShot shot) {
        AiModelConfig model = aiModelResolveService.resolve("VIDEO", OfficialVideoLane.MODEL);
        int duration = shot.getShotDurationSec() == null ? 3 : Math.max(1, shot.getShotDurationSec());
        String unit = model.getPriceUnit() == null ? "PER_SECOND" : model.getPriceUnit();
        int usage = "PER_SECOND".equals(unit) ? duration : 1;
        int cost = aiModelResolveService.costCent(model, usage);
        return cost > 0 ? cost : TaskPrices.cost("VIDEO") * usage;
    }

    private int officialUnit() {
        AiModelConfig model = aiModelResolveService.resolve("VIDEO", OfficialVideoLane.MODEL);
        int price = model.getUnitPriceCent() == null ? 0 : model.getUnitPriceCent();
        if (price > 0) {
            return price;
        }
        int fallback = aiModelResolveService.costCent(model, 1);
        return fallback > 0 ? fallback : TaskPrices.cost("VIDEO");
    }

    private void scan(ShortShot shot) {
        Map<String, Object> scan = ComplianceLexicon.scan(
                (shot.getDialogueText() == null ? "" : shot.getDialogueText()) + " "
                        + (shot.getImagePrompt() == null ? "" : shot.getImagePrompt()),
                shot.getSceneNo(), null, shot.getShotNo());
        if ("BLOCK".equals(scan.get("risk_level"))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "shot " + shot.getShotNo() + " BLOCK");
        }
    }

    private AiTaskReq singleOrMany(AiTaskReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        if ((req.getShotIds() == null || req.getShotIds().isEmpty())
                && req.getShotId() != null && !req.getShotId().isBlank()) {
            AiTaskReq copy = new AiTaskReq();
            copy.setEpisodeId(req.getEpisodeId());
            copy.setShotIds(List.of(req.getShotId().trim()));
            return copy;
        }
        return req;
    }

    private Map<String, Object> wallet(String userId) {
        UserQuota quota = quotaWalletService.requireWallet(userId);
        quotaWalletService.resetMonthIfNeeded(quota);
        int balance = quota.getBalanceCent() == null ? 0 : quota.getBalanceCent();
        int extra = quota.getExtraBalanceCent() == null ? 0 : quota.getExtraBalanceCent();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance_cent", balance);
        result.put("extra_balance_cent", extra);
        result.put("available_cent", balance + extra);
        return result;
    }

    private static List<Map<String, Object>> views(List<Plan> plans) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Plan plan : plans) {
            rows.add(view(plan));
        }
        return rows;
    }

    private static Map<String, Object> view(Plan plan) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("shot_id", plan.shot.getShotId());
        item.put("shot_no", plan.shot.getShotNo());
        item.put("route", plan.route);
        item.put("route_name", plan.routeName);
        item.put("size_note", plan.sizeNote);
        item.put("blocked", plan.blocked ? 1 : 0);
        item.put("block_reason", plan.blocked ? plan.blockReason : "");
        item.put("estimated_cost_cent", plan.costCent);
        item.put("video_asset_id", plan.shot.getVideoAssetId());
        return item;
    }

    private static final class Plan {
        private ShortShot shot;
        private String route = "";
        private String routeName = "";
        private String sizeNote = "";
        private String blockReason = "";
        private String assetId;
        private String prompt;
        private int costCent;
        private boolean blocked;
    }
}
