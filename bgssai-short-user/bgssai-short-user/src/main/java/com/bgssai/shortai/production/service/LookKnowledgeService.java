package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortLookLibraryMapper;
import com.bgssai.shortai.mapper.ShortLookMaterialMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortLookLibrary;
import com.bgssai.shortai.model.ShortLookLibraryExample;
import com.bgssai.shortai.model.ShortLookMaterial;
import com.bgssai.shortai.model.ShortLookMaterialExample;
import com.bgssai.shortai.production.dto.LookDtos.LookCompleteReq;
import com.bgssai.shortai.production.dto.LookDtos.LookLibraryReq;
import com.bgssai.shortai.production.dto.LookDtos.LookMaterialReq;
import com.bgssai.shortai.production.support.LookAngle;
import com.bgssai.shortai.production.support.LookImage;
import com.bgssai.shortai.production.support.LookViewPrompt;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.TaskPrices;
import com.bgssai.shortai.quota.service.QuotaConsumeService;
import com.bgssai.shortai.storage.ObjectStorage;

/**
 * 定妆知识库。形态对照多知识库容器：建库、上传图片素材、挂到定妆生成。
 * 图片说明写入 Elasticsearch 做检索。生成时先用这张卡的图，没有再按外观描述召回。
 */
@Service
public class LookKnowledgeService {

    public static final String DEFAULT_NAME = "我的定妆";

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortLookLibraryMapper libraryMapper;
    @Autowired
    private ShortLookMaterialMapper materialMapper;
    @Autowired
    private ObjectStorage objectStorage;
    @Autowired
    private Environment environment;
    @Autowired(required = false)
    private LookRagIndex lookRagIndex;
    @Autowired(required = false)
    private AiTaskQueue aiTaskQueue;
    @Autowired(required = false)
    private AiModelResolveService aiModelResolveService;
    @Autowired(required = false)
    private QuotaConsumeService quotaConsumeService;
    @Autowired(required = false)
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired(required = false)
    private ShortAssetMapper shortAssetMapper;

    public record LookHit(String materialId, String storageKey, String mimeType, String caption,
            String libraryId, String libraryName, String materialName, String viewAngle) {
        public LookHit(String materialId, String storageKey, String mimeType, String caption) {
            this(materialId, storageKey, mimeType, caption, null, null, null, null);
        }
    }

    public ApiResponse list() {
        String userId = workAccessService.requireUser();
        List<ShortLookLibrary> libraries = librariesOf(userId);
        List<Map<String, Object>> views = new ArrayList<>();
        for (ShortLookLibrary library : libraries) {
            views.add(libraryView(library, materialsOf(library.getLibraryId(), userId)));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("library_list", views);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse saveLibrary(LookLibraryReq req) {
        String userId = workAccessService.requireUser();
        String name = req == null || req.getLibraryName() == null ? "" : req.getLibraryName().trim();
        if (name.isEmpty() || name.length() > 120) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "知识库名称需要 1 到 120 个字");
        }
        String description = blankToNull(req.getDescription());
        if (description != null && description.length() > 500) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "说明最多 500 个字");
        }
        String status = req.getStatus() == null || req.getStatus().isBlank() ? "ACTIVE" : req.getStatus().trim().toUpperCase();
        if (!"ACTIVE".equals(status) && !"ARCHIVED".equals(status)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "状态只能是启用或归档");
        }
        int mount = req.getMountPortrait() != null && req.getMountPortrait() == 0 ? 0 : 1;
        Date now = new Date();
        if (req.getLibraryId() == null || req.getLibraryId().isBlank()) {
            ShortLookLibrary row = new ShortLookLibrary();
            row.setLibraryId(ShortIds.id("lk-"));
            row.setUserId(userId);
            row.setLibraryName(name);
            row.setDescription(description);
            row.setStatus(status);
            row.setMountPortrait(mount);
            row.setItemCount(0);
            row.setUsageBytes(0L);
            row.setDeletedFlag(0);
            row.setCreatedTime(now);
            row.setUpdatedTime(now);
            libraryMapper.insertSelective(row);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("library_id", row.getLibraryId());
            return Responses.ok(result);
        }
        ShortLookLibrary existing = requireLibrary(userId, req.getLibraryId());
        ShortLookLibrary patch = new ShortLookLibrary();
        patch.setLibraryName(name);
        patch.setDescription(description == null ? "" : description);
        patch.setStatus(status);
        patch.setMountPortrait(mount);
        patch.setUpdatedTime(now);
        ShortLookLibraryExample up = new ShortLookLibraryExample();
        up.createCriteria().andIdEqualTo(existing.getId());
        libraryMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("library_id", existing.getLibraryId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse upload(LookMaterialReq req) {
        String userId = workAccessService.requireUser();
        ShortLookLibrary library = req == null || req.getLibraryId() == null || req.getLibraryId().isBlank()
                ? defaultLibrary(userId)
                : requireLibrary(userId, req.getLibraryId());
        if (!"ACTIVE".equals(library.getStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "已归档的知识库不能再放素材");
        }
        LookImage.Decoded image = LookImage.decode(req.getImageBase64());
        LookImage.Decoded poster = posterOf(req.getPosterBase64(), image.mimeType());
        String kind = normalizeKind(req.getMaterialKind());
        String name = req.getMaterialName() == null || req.getMaterialName().isBlank() ? "未命名定妆" : req.getMaterialName().trim();
        if (name.length() > 120) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "素材名称最多 120 个字");
        }
        String caption = blankToNull(req.getCaption());
        if (caption != null && caption.length() > 500) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "说明最多 500 个字");
        }
        String angle = characterAngle(kind, req.getViewAngle(), caption);
        ShortLookMaterial saved = store(library, userId, kind, name, caption, image, poster, angle, null, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("material_id", saved.getMaterialId());
        result.put("library_id", library.getLibraryId());
        result.put("file_url", presign(saved.getStorageKey()));
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse deleteMaterial(LookMaterialReq req) {
        String userId = workAccessService.requireUser();
        if (req == null || req.getMaterialId() == null || req.getMaterialId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请选择要移除的素材");
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andMaterialIdEqualTo(req.getMaterialId()).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ShortLookMaterial row = materialMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "素材不存在");
        }
        ShortLookMaterial patch = new ShortLookMaterial();
        patch.setDeletedFlag(1);
        patch.setUpdatedTime(new Date());
        ShortLookMaterialExample up = new ShortLookMaterialExample();
        up.createCriteria().andIdEqualTo(row.getId());
        materialMapper.updateByExampleSelective(patch, up);
        ShortLookLibrary library = requireLibrary(userId, row.getLibraryId());
        ShortLookLibrary libPatch = new ShortLookLibrary();
        libPatch.setItemCount(Math.max(0, (library.getItemCount() == null ? 0 : library.getItemCount()) - 1));
        long usage = library.getUsageBytes() == null ? 0L : library.getUsageBytes();
        int size = row.getFileSizeByte() == null ? 0 : row.getFileSizeByte();
        libPatch.setUsageBytes(Math.max(0L, usage - size));
        libPatch.setUpdatedTime(new Date());
        ShortLookLibraryExample libUp = new ShortLookLibraryExample();
        libUp.createCriteria().andIdEqualTo(library.getId());
        libraryMapper.updateByExampleSelective(libPatch, libUp);
        if (lookRagIndex != null) {
            lookRagIndex.remove(row.getMaterialId());
        }
        retireSameKey(userId, row.getStorageKey(), row.getId());
        return Responses.ok("ok");
    }

    /** Card upload files the same photo into 「我的定妆」, creating that library when it is missing. */
    @Transactional
    public void fileImage(String userId, String kind, String name, String caption, LookImage.Decoded image,
            String viewAngle, String cardId, String assetId) {
        ShortLookLibrary library = personalLibrary(userId);
        String normalizedKind = normalizeKind(kind);
        String angle = characterAngle(normalizedKind, viewAngle, caption);
        if (angle == null) {
            replaceCardAll(userId, cardId);
        } else {
            replaceCardAngle(userId, cardId, angle);
        }
        store(library, userId, normalizedKind, name == null || name.isBlank() ? "未命名定妆" : name.trim(),
                blankToNull(caption), image, null, angle, cardId, assetId);
    }

    public ShortLookMaterial requireMaterial(String userId, String materialId) {
        if (materialId == null || materialId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请选择一个造型");
        }
        ShortLookMaterial row = findMaterial(userId, materialId);
        if (row == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "造型不存在");
        }
        return row;
    }

    public List<ShortLookMaterial> lookGroup(ShortLookMaterial source) {
        if (source == null || materialMapper == null) {
            return List.of();
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(source.getUserId()).andLibraryIdEqualTo(source.getLibraryId())
                .andMaterialKindEqualTo(source.getMaterialKind()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<ShortLookMaterial> rows = materialMapper.selectByExample(ex);
        if (rows == null) {
            return List.of();
        }
        String name = source.getMaterialName() == null ? "" : source.getMaterialName();
        List<ShortLookMaterial> group = new ArrayList<>();
        for (ShortLookMaterial row : rows) {
            String rowName = row.getMaterialName() == null ? "" : row.getMaterialName();
            if (name.equals(rowName)) {
                group.add(row);
            }
        }
        return group;
    }

    public LookImage.Decoded imageOf(ShortLookMaterial row) {
        if (row == null || row.getStorageKey() == null || row.getStorageKey().isBlank()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "这张参考图读不出来，请换一张");
        }
        String key = row.getStorageKey();
        String mime = row.getMimeType() == null ? "image/jpeg" : row.getMimeType();
        int width = row.getWidth() == null ? 0 : row.getWidth();
        int height = row.getHeight() == null ? 0 : row.getHeight();
        if (mime.startsWith("video/")) {
            key = posterKey(key);
            mime = "image/jpeg";
            width = 0;
            height = 0;
        }
        byte[] bytes;
        try {
            bytes = objectStorage.get(key);
        } catch (RuntimeException ex) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "这张参考图读不出来，请换一张");
        }
        if (bytes == null || bytes.length == 0) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "这张参考图读不出来，请换一张");
        }
        return new LookImage.Decoded(bytes, mime, width, height);
    }

    /** Point this card at an existing look. Copies no bytes; the photos stay in the library. */
    @Transactional
    public void bindLook(String userId, String cardId, String primaryMaterialId, String primaryAssetId,
            List<ShortLookMaterial> group) {
        replaceCardAll(userId, cardId);
        Map<String, ShortLookMaterial> newest = new LinkedHashMap<>();
        if (group != null) {
            for (ShortLookMaterial row : group) {
                String angle = row.getViewAngle() == null ? "" : row.getViewAngle();
                newest.putIfAbsent(angle, row);
            }
        }
        for (ShortLookMaterial row : newest.values()) {
            String assetId = row.getMaterialId() != null && row.getMaterialId().equals(primaryMaterialId)
                    ? primaryAssetId : null;
            insertPointer(row, cardId, assetId);
        }
    }

    public Map<String, Object> preview(String userId, String kind, String cardId, String prompt) {
        ShortLookMaterial onCard = cardMaterial(userId, cardId);
        if (onCard != null) {
            return previewMap(userId, onCard, "CARD");
        }
        LookHit named = bestMatch(userId, kind, prompt);
        if (named != null) {
            ShortLookMaterial row = findMaterial(userId, named.materialId());
            if (row != null) {
                return previewMap(userId, row, "NAME");
            }
            return previewMap(named, "NAME");
        }
        Map<String, Object> none = new LinkedHashMap<>();
        none.put("used", 0);
        none.put("source", "NONE");
        return none;
    }

    public List<Map<String, Object>> views(String userId, String cardId) {
        if (userId == null || cardId == null || cardId.isBlank() || materialMapper == null) {
            return List.of();
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andCardIdEqualTo(cardId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<ShortLookMaterial> rows = materialMapper.selectByExample(ex);
        if (rows == null) {
            return List.of();
        }
        Map<String, ShortLookMaterial> latest = new LinkedHashMap<>();
        for (String angle : List.of("FRONT_FACE", "FRONT", "BACK", "SIDE")) {
            latest.put(angle, null);
        }
        for (ShortLookMaterial row : rows) {
            String angle = shownAngle(row);
            if (angle == null || !latest.containsKey(angle)) {
                continue;
            }
            ShortLookMaterial current = latest.get(angle);
            if (current == null) {
                latest.put(angle, row);
                continue;
            }
            if (LookAngle.poseExtra(current.getCaption()) && !LookAngle.poseExtra(row.getCaption())) {
                latest.put(angle, row);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, ShortLookMaterial> entry : latest.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("view_angle", entry.getKey());
            item.put("view_name", viewName(entry.getKey()));
            ShortLookMaterial row = entry.getValue();
            item.put("asset_id", row == null ? null : row.getAssetId());
            item.put("file_url", row == null ? null : presign(row.getStorageKey()));
            list.add(item);
        }
        return list;
    }

    public static String normalizeView(String raw, boolean required) {
        if (raw == null || raw.isBlank()) {
            return required ? "FRONT_FACE" : null;
        }
        String value = raw.trim().toUpperCase();
        if (!List.of("FRONT_FACE", "FRONT", "BACK", "SIDE").contains(value)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "定妆角度只能是正脸、前面、背面、侧面");
        }
        return value;
    }

    public static String viewName(String angle) {
        return switch (angle) {
            case "FRONT" -> "前面";
            case "BACK" -> "背面";
            case "SIDE" -> "侧面";
            default -> "正脸";
        };
    }

    static int rankScore(int score, String viewAngle) {
        if (score < 3) {
            return score;
        }
        return "FRONT_FACE".equals(viewAngle) ? score + 15 : score;
    }

    /** Photos already filed on this card. Front face wins, then front, back, side. */
    public LookHit referenceForCard(String userId, String cardId) {
        return toHit(userId, cardMaterial(userId, cardId));
    }

    private ShortLookMaterial cardMaterial(String userId, String cardId) {
        if (userId == null || userId.isBlank() || cardId == null || cardId.isBlank() || materialMapper == null) {
            return null;
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andCardIdEqualTo(cardId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        return pickCardReference(materialMapper.selectByExample(ex));
    }

    static ShortLookMaterial pickCardReference(List<ShortLookMaterial> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        for (String angle : List.of("FRONT_FACE", "FRONT", "BACK", "SIDE")) {
            for (ShortLookMaterial row : rows) {
                if (angle.equals(row.getViewAngle()) && hasBytes(row)) {
                    return row;
                }
            }
        }
        for (ShortLookMaterial row : rows) {
            if (hasBytes(row)) {
                return row;
            }
        }
        return null;
    }

    private static boolean hasBytes(ShortLookMaterial row) {
        return row != null && row.getStorageKey() != null && !row.getStorageKey().isBlank();
    }

    public LookHit bestMatch(String userId, String kind, String prompt) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        String wanted = normalizeKind(kind);
        Set<String> mounted = new LinkedHashSet<>();
        for (ShortLookLibrary library : librariesOf(userId)) {
            if ("ACTIVE".equals(library.getStatus()) && library.getMountPortrait() != null && library.getMountPortrait() == 1) {
                mounted.add(library.getLibraryId());
            }
        }
        if (mounted.isEmpty()) {
            return null;
        }
        if (lookRagIndex != null) {
            LookHit retrieved = lookRagIndex.search(userId, mounted, wanted, prompt);
            if (retrieved != null) {
                ShortLookMaterial row = findMaterial(userId, retrieved.materialId());
                LookHit full = toHit(userId, row);
                if (full != null) {
                    return full;
                }
            }
        }
        ShortLookMaterial best = null;
        int bestScore = 0;
        for (ShortLookMaterial row : materialsOfUser(userId)) {
            if (!mounted.contains(row.getLibraryId()) || !wanted.equals(row.getMaterialKind())) {
                continue;
            }
            int score = rankScore(matchScore(prompt, row.getMaterialName(), row.getCaption()), row.getViewAngle());
            if (score > bestScore) {
                bestScore = score;
                best = row;
            }
        }
        if (best == null || bestScore < 3 || best.getStorageKey() == null || best.getStorageKey().isBlank()) {
            return null;
        }
        return toHit(userId, best);
    }

    static int matchScore(String prompt, String name, String caption) {
        String needle = prompt == null ? "" : prompt.trim();
        String title = name == null ? "" : name.trim();
        String note = caption == null ? "" : caption.trim();
        String hay = (title + " " + note).trim();
        if (needle.isEmpty() || hay.isEmpty()) {
            return 0;
        }
        int score = sharedHan(needle, hay);
        if (!title.isEmpty() && (needle.contains(title) || title.contains(needle))) {
            score += 20;
        }
        return score;
    }

    private static int sharedHan(String left, String right) {
        Set<Integer> chars = new LinkedHashSet<>();
        collectHan(left, chars);
        if (chars.isEmpty()) {
            return 0;
        }
        Set<Integer> hit = new LinkedHashSet<>();
        int index = 0;
        while (index < right.length()) {
            int cp = right.codePointAt(index);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN && chars.contains(cp)) {
                hit.add(cp);
            }
            index += Character.charCount(cp);
        }
        return hit.size();
    }

    private static void collectHan(String text, Set<Integer> out) {
        int index = 0;
        while (index < text.length()) {
            int cp = text.codePointAt(index);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                out.add(cp);
            }
            index += Character.charCount(cp);
        }
    }

    private void replaceCardAngle(String userId, String cardId, String angle) {
        if (cardId == null || cardId.isBlank() || angle == null) {
            return;
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andCardIdEqualTo(cardId).andViewAngleEqualTo(angle).andDeletedFlagEqualTo(0);
        retire(materialMapper.selectByExample(ex));
    }

    private void replaceCardAll(String userId, String cardId) {
        if (cardId == null || cardId.isBlank() || materialMapper == null) {
            return;
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andCardIdEqualTo(cardId).andDeletedFlagEqualTo(0);
        retire(materialMapper.selectByExample(ex));
    }

    private void retireSameKey(String userId, String storageKey, Long exceptId) {
        if (storageKey == null || storageKey.isBlank() || materialMapper == null) {
            return;
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        List<ShortLookMaterial> rows = materialMapper.selectByExample(ex);
        if (rows == null) {
            return;
        }
        List<ShortLookMaterial> extra = new ArrayList<>();
        for (ShortLookMaterial row : rows) {
            if (exceptId != null && exceptId.equals(row.getId())) {
                continue;
            }
            if (storageKey.equals(row.getStorageKey())) {
                extra.add(row);
            }
        }
        retire(extra);
    }

    private void retire(List<ShortLookMaterial> rows) {
        if (rows == null) {
            return;
        }
        for (ShortLookMaterial row : rows) {
            ShortLookMaterial patch = new ShortLookMaterial();
            patch.setDeletedFlag(1);
            patch.setUpdatedTime(new Date());
            ShortLookMaterialExample up = new ShortLookMaterialExample();
            up.createCriteria().andIdEqualTo(row.getId());
            materialMapper.updateByExampleSelective(patch, up);
            if (lookRagIndex != null) {
                lookRagIndex.remove(row.getMaterialId());
            }
        }
    }

    private void insertPointer(ShortLookMaterial source, String cardId, String assetId) {
        ShortLookMaterial row = new ShortLookMaterial();
        row.setMaterialId(ShortIds.id("lm-"));
        row.setLibraryId(source.getLibraryId());
        row.setUserId(source.getUserId());
        row.setMaterialName(source.getMaterialName());
        row.setCaption(source.getCaption());
        row.setMaterialKind(source.getMaterialKind());
        row.setViewAngle(source.getViewAngle());
        row.setCardId(cardId);
        row.setAssetId(assetId);
        row.setMimeType(source.getMimeType());
        row.setStorageKey(source.getStorageKey());
        row.setWidth(source.getWidth());
        row.setHeight(source.getHeight());
        row.setFileSizeByte(source.getFileSizeByte() == null ? 0 : source.getFileSizeByte());
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        materialMapper.insertSelective(row);
        if (lookRagIndex != null) {
            lookRagIndex.upsert(row);
        }
    }

    private LookHit toHit(String userId, ShortLookMaterial row) {
        if (row == null || row.getStorageKey() == null || row.getStorageKey().isBlank()) {
            return null;
        }
        return new LookHit(row.getMaterialId(), row.getStorageKey(), row.getMimeType(), row.getCaption(),
                row.getLibraryId(), libraryName(userId, row.getLibraryId()), row.getMaterialName(), row.getViewAngle());
    }

    private Map<String, Object> previewMap(String userId, ShortLookMaterial row, String source) {
        LookHit hit = toHit(userId, row);
        if (hit == null) {
            Map<String, Object> none = new LinkedHashMap<>();
            none.put("used", 0);
            none.put("source", "NONE");
            return none;
        }
        Map<String, Object> item = previewMap(hit, source);
        item.put("asset_id", row.getAssetId());
        return item;
    }

    private Map<String, Object> previewMap(LookHit hit, String source) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("used", 1);
        item.put("source", source);
        item.put("library_id", hit.libraryId());
        item.put("library_name", hit.libraryName());
        item.put("material_id", hit.materialId());
        item.put("material_name", hit.materialName());
        item.put("view_angle", hit.viewAngle());
        item.put("view_name", hit.viewAngle() == null || hit.viewAngle().isBlank() ? null : viewName(hit.viewAngle()));
        item.put("mime_type", hit.mimeType());
        item.put("file_url", presign(hit.storageKey()));
        if (hit.mimeType() != null && hit.mimeType().startsWith("video/")) {
            item.put("poster_url", presign(posterKey(hit.storageKey())));
        }
        return item;
    }

    private ShortLookMaterial findMaterial(String userId, String materialId) {
        if (userId == null || materialId == null || materialId.isBlank() || materialMapper == null) {
            return null;
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andMaterialIdEqualTo(materialId).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        return materialMapper.selectOneByExample(ex);
    }

    private String libraryName(String userId, String libraryId) {
        if (userId == null || libraryId == null || libraryId.isBlank() || libraryMapper == null) {
            return null;
        }
        ShortLookLibraryExample ex = new ShortLookLibraryExample();
        ex.createCriteria().andLibraryIdEqualTo(libraryId).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ShortLookLibrary row = libraryMapper.selectOneByExample(ex);
        return row == null ? null : row.getLibraryName();
    }

    static String posterKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        int slash = Math.max(storageKey.lastIndexOf('/'), storageKey.lastIndexOf('\\'));
        int dot = storageKey.lastIndexOf('.');
        if (dot > slash) {
            return storageKey.substring(0, dot) + ".poster.jpg";
        }
        return storageKey + ".poster.jpg";
    }

    private static LookImage.Decoded posterOf(String raw, String mime) {
        if (mime == null || !mime.startsWith("video/") || raw == null || raw.isBlank()) {
            return null;
        }
        LookImage.Decoded poster = LookImage.decode(raw);
        if (poster.mimeType().startsWith("video/")) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "视频封面需要是图片");
        }
        return poster;
    }

    private ShortLookMaterial store(ShortLookLibrary library, String userId, String kind, String name, String caption,
            LookImage.Decoded image, LookImage.Decoded poster, String viewAngle, String cardId, String assetId) {
        ShortLookMaterial row = new ShortLookMaterial();
        row.setMaterialId(ShortIds.id("lm-"));
        row.setLibraryId(library.getLibraryId());
        row.setUserId(userId);
        row.setMaterialName(name);
        row.setCaption(caption);
        row.setMaterialKind(kind);
        row.setViewAngle(viewAngle);
        row.setCardId(cardId);
        row.setAssetId(assetId);
        row.setMimeType(image.mimeType());
        String key = "short/" + activeEnv() + "/" + userId + "/look/" + row.getMaterialId() + "."
                + LookImage.extension(image.mimeType());
        objectStorage.put(key, image.bytes(), image.mimeType());
        row.setStorageKey(key);
        LookImage.Decoded measured = poster != null ? poster : image;
        if (poster != null) {
            objectStorage.put(posterKey(key), poster.bytes(), poster.mimeType());
        }
        if (measured.width() > 0) {
            row.setWidth(measured.width());
        }
        if (measured.height() > 0) {
            row.setHeight(measured.height());
        }
        row.setFileSizeByte(image.bytes().length);
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        materialMapper.insertSelective(row);
        ShortLookLibrary patch = new ShortLookLibrary();
        patch.setItemCount((library.getItemCount() == null ? 0 : library.getItemCount()) + 1);
        int posterBytes = poster == null ? 0 : poster.bytes().length;
        patch.setUsageBytes((library.getUsageBytes() == null ? 0L : library.getUsageBytes())
                + image.bytes().length + posterBytes);
        patch.setUpdatedTime(new Date());
        ShortLookLibraryExample up = new ShortLookLibraryExample();
        up.createCriteria().andIdEqualTo(library.getId());
        libraryMapper.updateByExampleSelective(patch, up);
        library.setItemCount(patch.getItemCount());
        library.setUsageBytes(patch.getUsageBytes());
        if (lookRagIndex != null) {
            lookRagIndex.upsert(row);
        }
        return row;
    }

    private ShortLookLibrary defaultLibrary(String userId) {
        return personalLibrary(userId);
    }

    private ShortLookLibrary personalLibrary(String userId) {
        for (ShortLookLibrary library : librariesOf(userId)) {
            if (DEFAULT_NAME.equals(library.getLibraryName())) {
                return library;
            }
        }
        ShortLookLibrary row = new ShortLookLibrary();
        row.setLibraryId(ShortIds.id("lk-"));
        row.setUserId(userId);
        row.setLibraryName(DEFAULT_NAME);
        row.setDescription("角色卡上上传的照片会放在这里，生成定妆时也可以直接选用。");
        row.setStatus("ACTIVE");
        row.setMountPortrait(1);
        row.setItemCount(0);
        row.setUsageBytes(0L);
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        libraryMapper.insertSelective(row);
        return row;
    }

    private List<ShortLookLibrary> librariesOf(String userId) {
        ShortLookLibraryExample ex = new ShortLookLibraryExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<ShortLookLibrary> rows = libraryMapper.selectByExample(ex);
        return rows == null ? List.of() : rows;
    }

    private List<ShortLookMaterial> materialsOf(String libraryId, String userId) {
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andLibraryIdEqualTo(libraryId).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<ShortLookMaterial> rows = materialMapper.selectByExample(ex);
        return rows == null ? List.of() : rows;
    }

    private List<ShortLookMaterial> materialsOfUser(String userId) {
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<ShortLookMaterial> rows = materialMapper.selectByExample(ex);
        return rows == null ? List.of() : rows;
    }

    private ShortLookLibrary requireLibrary(String userId, String libraryId) {
        ShortLookLibraryExample ex = new ShortLookLibraryExample();
        ex.createCriteria().andLibraryIdEqualTo(libraryId).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ShortLookLibrary row = libraryMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "知识库不存在");
        }
        return row;
    }

    private Map<String, Object> libraryView(ShortLookLibrary library, List<ShortLookMaterial> materials) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("library_id", library.getLibraryId());
        item.put("library_name", library.getLibraryName());
        item.put("description", library.getDescription());
        item.put("status", library.getStatus());
        item.put("mount_portrait", library.getMountPortrait() == null ? 0 : library.getMountPortrait());
        item.put("item_count", library.getItemCount() == null ? 0 : library.getItemCount());
        List<Map<String, Object>> files = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ShortLookMaterial material : materials) {
            String angle = shownAngle(material);
            String marker = (material.getMaterialName() == null ? "" : material.getMaterialName())
                    + "\n" + (angle == null ? "" : angle)
                    + "\n" + (material.getStorageKey() == null ? "" : material.getStorageKey());
            if (!seen.add(marker)) {
                continue;
            }
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("material_id", material.getMaterialId());
            file.put("material_name", material.getMaterialName());
            file.put("caption", material.getCaption());
            file.put("material_kind", material.getMaterialKind());
            file.put("view_angle", angle);
            file.put("view_name", angle == null ? null : viewName(angle));
            file.put("mime_type", material.getMimeType());
            file.put("file_url", presign(material.getStorageKey()));
            if (material.getMimeType() != null && material.getMimeType().startsWith("video/")) {
                file.put("poster_url", presign(posterKey(material.getStorageKey())));
            }
            file.put("width", material.getWidth());
            file.put("height", material.getHeight());
            file.put("file_size_byte", material.getFileSizeByte() == null ? 0 : material.getFileSizeByte());
            files.add(file);
        }
        item.put("material_list", files);
        return item;
    }

    private String presign(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return objectStorage.presignGet(key, 1800).downloadUrl();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String activeEnv() {
        String[] profiles = environment == null ? null : environment.getActiveProfiles();
        if (profiles != null && profiles.length > 0 && profiles[0] != null && !profiles[0].isBlank()) {
            return profiles[0];
        }
        return "local";
    }

    /**
     * Fill or redraw 前面 / 背面 / 侧面 from the look's 正脸. confirm_cost=1 enqueues the image task.
     * Without that flag the call only returns the angles and the price.
     */
    @Transactional
    public ApiResponse completeAngles(LookCompleteReq req) {
        String userId = workAccessService.requireUser();
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请选择要补的造型");
        }
        boolean confirm = req.getConfirmCost() != null && req.getConfirmCost() == 1;
        AngleTarget target = resolveTarget(userId, req, confirm);
        boolean replace = req.getReplaceExisting() != null && req.getReplaceExisting() == 1;
        List<String> angles = LookAngle.plan(target.filled(), req.getViewAngle(), replace);
        if (aiTaskQueue == null || aiModelResolveService == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "出图还没准备好，请稍后再试");
        }
        String modelCode = req.getModelCode() == null ? null : req.getModelCode().trim();
        aiTaskQueue.ensurePipeline("IMAGE", modelCode);
        AiModelConfig model = aiModelResolveService.resolve("IMAGE", modelCode);
        int usage = angles.size();
        int priced = aiModelResolveService.costCent(model, usage);
        int cost = priced > 0 ? priced : TaskPrices.cost("IMAGE") * usage;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("library_id", target.libraryId());
        result.put("material_name", target.materialName());
        result.put("angle_list", angles);
        List<String> names = new ArrayList<>();
        for (String angle : angles) {
            names.add(viewName(angle));
        }
        result.put("angle_name_list", names);
        result.put("estimated_cost_cent", cost);
        result.put("reference_view_name", "正脸");
        if (!confirm) {
            result.put("confirmed", 0);
            return Responses.ok(result);
        }
        if (quotaConsumeService != null) {
            quotaConsumeService.assertAffordable(userId, cost);
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("library_id", target.libraryId());
        request.put("material_name", target.materialName());
        request.put("angles", angles);
        request.put("appearance", target.appearance());
        request.put("reference_storage_key", target.storageKey());
        request.put("reference_mime", target.mime());
        request.put("usage_count", usage);
        if (target.cardId() != null) {
            request.put("card_id", target.cardId());
        }
        if (modelCode != null && !modelCode.isBlank()) {
            request.put("model_code", modelCode);
        }
        ShortAiTask task = aiTaskQueue.enqueue(userId, target.workId(), null, "IMAGE", "LOOK_ANGLE",
                target.libraryId(), request, null, 0, modelCode);
        result.put("confirmed", 1);
        result.put("task_id", task.getTaskId());
        result.put("batch_id", task.getBatchId());
        return Responses.ok(result);
    }

    @Transactional
    public void saveGenerated(String userId, String libraryId, String name, String caption, String angle,
            byte[] bytes, String mime, int width, int height, String cardId) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR, "这一面没有生成出来");
        }
        String stored = LookAngle.canonical(angle);
        if (stored == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "定妆角度只能是正脸、前面、背面、侧面");
        }
        ShortLookLibrary library = requireLibrary(userId, libraryId);
        if (!"ACTIVE".equals(library.getStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "已归档的知识库不能再放素材");
        }
        String safeMime = mime == null || mime.isBlank() ? "image/png" : mime;
        LookImage.Decoded image = new LookImage.Decoded(bytes, safeMime, Math.max(0, width), Math.max(0, height));
        String title = name == null || name.isBlank() ? "未命名定妆" : name.trim();
        store(library, userId, "CHARACTER", title, blankToNull(caption), image, null, stored, blankToNull(cardId), null);
    }

    private AngleTarget resolveTarget(String userId, LookCompleteReq req, boolean createLibrary) {
        if (req.getCharacterId() != null && !req.getCharacterId().isBlank()) {
            return characterTarget(userId, req.getCharacterId().trim(), createLibrary);
        }
        String libraryId = req.getLibraryId() == null ? "" : req.getLibraryId().trim();
        String name = req.getMaterialName() == null ? "" : req.getMaterialName().trim();
        if (libraryId.isEmpty() || name.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请选择要补的造型");
        }
        ShortLookLibrary library = requireLibrary(userId, libraryId);
        if (!"ACTIVE".equals(library.getStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "已归档的知识库不能再放素材");
        }
        List<ShortLookMaterial> rows = namedMaterials(materialsOf(libraryId, userId), name);
        ShortLookMaterial face = pickFace(rows);
        if (face == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "先有一张正脸，才能按这张脸补齐其他面");
        }
        return new AngleTarget(libraryId, name, null, null, face.getStorageKey(), faceMime(face),
                face.getCaption(), filledAngles(rows));
    }

    private AngleTarget characterTarget(String userId, String characterId, boolean createLibrary) {
        if (shortCharacterMapper == null) {
            throw new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "出图还没准备好，请稍后再试");
        }
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andCharacterIdEqualTo(characterId).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ShortCharacter character = shortCharacterMapper.selectOneByExample(ex);
        if (character == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "角色不存在");
        }
        List<ShortLookMaterial> rows = cardMaterials(userId, characterId);
        ShortLookMaterial face = pickFace(rows);
        String appearance = firstText(character.getAppearancePrompt(), face == null ? null : face.getCaption());
        if (face != null) {
            ShortLookLibrary library = requireLibrary(userId, face.getLibraryId());
            if (!"ACTIVE".equals(library.getStatus())) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "已归档的知识库不能再放素材");
            }
            String materialName = face.getMaterialName();
            if (materialName == null || materialName.isBlank()) {
                materialName = character.getCharacterName() == null || character.getCharacterName().isBlank()
                        ? "未命名定妆" : character.getCharacterName().trim();
            }
            return new AngleTarget(face.getLibraryId(), materialName, characterId, character.getWorkId(),
                    face.getStorageKey(), faceMime(face), appearance, filledAngles(rows));
        }
        ShortAsset asset = loadAsset(userId, character.getRefImageAssetId());
        if (asset == null || !objectKey(asset.getStorageKey())
                || (asset.getMimeType() != null && asset.getMimeType().startsWith("video/"))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "先有一张正脸，才能按这张脸补齐其他面");
        }
        String libraryId = personalLibraryId(userId, createLibrary);
        String name = character.getCharacterName() == null || character.getCharacterName().isBlank()
                ? "未命名定妆" : character.getCharacterName().trim();
        Set<String> filled = filledAngles(rows);
        filled.add("FRONT_FACE");
        return new AngleTarget(libraryId, name, characterId, character.getWorkId(), asset.getStorageKey(),
                asset.getMimeType() == null ? "image/jpeg" : asset.getMimeType(), appearance, filled);
    }

    private String personalLibraryId(String userId, boolean create) {
        for (ShortLookLibrary library : librariesOf(userId)) {
            if (DEFAULT_NAME.equals(library.getLibraryName())) {
                if (!"ACTIVE".equals(library.getStatus())) {
                    throw new BizException(CommonErrorCode.RULE_REJECT, "已归档的知识库不能再放素材");
                }
                return library.getLibraryId();
            }
        }
        if (!create) {
            return null;
        }
        return personalLibrary(userId).getLibraryId();
    }

    private List<ShortLookMaterial> cardMaterials(String userId, String cardId) {
        if (materialMapper == null) {
            return List.of();
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andCardIdEqualTo(cardId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<ShortLookMaterial> rows = materialMapper.selectByExample(ex);
        return rows == null ? List.of() : rows;
    }

    private static List<ShortLookMaterial> namedMaterials(List<ShortLookMaterial> rows, String name) {
        List<ShortLookMaterial> group = new ArrayList<>();
        for (ShortLookMaterial row : rows) {
            if (!"CHARACTER".equals(row.getMaterialKind())) {
                continue;
            }
            String rowName = row.getMaterialName() == null ? "" : row.getMaterialName();
            if (name.equals(rowName)) {
                group.add(row);
            }
        }
        return group;
    }

    private static ShortLookMaterial pickFace(List<ShortLookMaterial> rows) {
        ShortLookMaterial plain = null;
        ShortLookMaterial pose = null;
        for (ShortLookMaterial row : rows) {
            if (!stillImage(row) || !"FRONT_FACE".equals(shownAngle(row))) {
                continue;
            }
            if (LookAngle.poseExtra(row.getCaption())) {
                if (pose == null) {
                    pose = row;
                }
            } else if (plain == null) {
                plain = row;
            }
        }
        return plain != null ? plain : pose;
    }

    private static Set<String> filledAngles(List<ShortLookMaterial> rows) {
        Set<String> filled = new LinkedHashSet<>();
        for (ShortLookMaterial row : rows) {
            if (!stillImage(row)) {
                continue;
            }
            String angle = shownAngle(row);
            if (angle != null) {
                filled.add(angle);
            }
        }
        return filled;
    }

    private static boolean stillImage(ShortLookMaterial row) {
        if (!hasBytes(row)) {
            return false;
        }
        String mime = row.getMimeType();
        return mime == null || !mime.startsWith("video/");
    }

    private static String shownAngle(ShortLookMaterial row) {
        if (row == null) {
            return null;
        }
        return LookAngle.resolve(row.getViewAngle(), row.getCaption());
    }

    private static String faceMime(ShortLookMaterial row) {
        String mime = row.getMimeType();
        return mime == null || mime.isBlank() ? "image/jpeg" : mime;
    }

    private static String characterAngle(String kind, String explicit, String caption) {
        if (!"CHARACTER".equals(kind)) {
            return null;
        }
        String angle = LookAngle.resolve(explicit, caption);
        return angle == null ? "FRONT_FACE" : angle;
    }

    private ShortAsset loadAsset(String userId, String assetId) {
        if (shortAssetMapper == null || assetId == null || assetId.isBlank()) {
            return null;
        }
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        return shortAssetMapper.selectOneByExample(ex);
    }

    private static boolean objectKey(String key) {
        return key != null && !key.isBlank() && !key.startsWith("<") && !key.startsWith("data:");
    }

    private static String firstText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private record AngleTarget(String libraryId, String materialName, String cardId, String workId,
            String storageKey, String mime, String appearance, Set<String> filled) {
    }

    private static String normalizeKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return "CHARACTER";
        }
        String value = kind.trim().toUpperCase();
        if (!"CHARACTER".equals(value) && !"SCENE".equals(value) && !"PROP".equals(value)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "素材类型只能是角色、场景或道具");
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
