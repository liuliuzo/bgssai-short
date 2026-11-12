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

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortLookLibraryMapper;
import com.bgssai.shortai.mapper.ShortLookMaterialMapper;
import com.bgssai.shortai.model.ShortLookLibrary;
import com.bgssai.shortai.model.ShortLookLibraryExample;
import com.bgssai.shortai.model.ShortLookMaterial;
import com.bgssai.shortai.model.ShortLookMaterialExample;
import com.bgssai.shortai.production.dto.LookDtos.LookLibraryReq;
import com.bgssai.shortai.production.dto.LookDtos.LookMaterialReq;
import com.bgssai.shortai.production.support.LookImage;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.storage.ObjectStorage;

/**
 * 定妆知识库。形态对照多知识库容器：建库、上传图片素材、挂到定妆生成。
 * 图片按名称和说明与外观描述对齐后，作为主体参考图交给图像模型。
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

    public record LookHit(String materialId, String storageKey, String mimeType, String caption) {
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
        String kind = normalizeKind(req.getMaterialKind());
        String name = req.getMaterialName() == null || req.getMaterialName().isBlank() ? "未命名定妆" : req.getMaterialName().trim();
        if (name.length() > 120) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "素材名称最多 120 个字");
        }
        String caption = blankToNull(req.getCaption());
        if (caption != null && caption.length() > 500) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "说明最多 500 个字");
        }
        String angle = "CHARACTER".equals(kind) ? normalizeView(req.getViewAngle(), true) : null;
        ShortLookMaterial saved = store(library, userId, kind, name, caption, image, angle, null, null);
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
        return Responses.ok("ok");
    }

    /** Card upload files the same photo into the creator's mounted look library. */
    @Transactional
    public void fileImage(String userId, String kind, String name, String caption, LookImage.Decoded image,
            String viewAngle, String cardId, String assetId) {
        ShortLookLibrary library = defaultLibrary(userId);
        if (!"ACTIVE".equals(library.getStatus()) || library.getMountPortrait() == null || library.getMountPortrait() == 0) {
            return;
        }
        String normalizedKind = normalizeKind(kind);
        String angle = "CHARACTER".equals(normalizedKind) ? normalizeView(viewAngle, true) : null;
        replaceCardAngle(userId, cardId, angle);
        store(library, userId, normalizedKind, name == null || name.isBlank() ? "未命名定妆" : name.trim(),
                blankToNull(caption), image, angle, cardId, assetId);
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
            if (row.getViewAngle() != null && latest.containsKey(row.getViewAngle()) && latest.get(row.getViewAngle()) == null) {
                latest.put(row.getViewAngle(), row);
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
        if (userId == null || userId.isBlank() || cardId == null || cardId.isBlank() || materialMapper == null) {
            return null;
        }
        ShortLookMaterialExample ex = new ShortLookMaterialExample();
        ex.createCriteria().andUserIdEqualTo(userId).andCardIdEqualTo(cardId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        ShortLookMaterial chosen = pickCardReference(materialMapper.selectByExample(ex));
        if (chosen == null) {
            return null;
        }
        return new LookHit(chosen.getMaterialId(), chosen.getStorageKey(), chosen.getMimeType(), chosen.getCaption());
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
        return new LookHit(best.getMaterialId(), best.getStorageKey(), best.getMimeType(), best.getCaption());
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
        List<ShortLookMaterial> rows = materialMapper.selectByExample(ex);
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
        }
    }

    private ShortLookMaterial store(ShortLookLibrary library, String userId, String kind, String name, String caption,
            LookImage.Decoded image, String viewAngle, String cardId, String assetId) {
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
        if (image.width() > 0) {
            row.setWidth(image.width());
        }
        if (image.height() > 0) {
            row.setHeight(image.height());
        }
        row.setFileSizeByte(image.bytes().length);
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        materialMapper.insertSelective(row);
        ShortLookLibrary patch = new ShortLookLibrary();
        patch.setItemCount((library.getItemCount() == null ? 0 : library.getItemCount()) + 1);
        patch.setUsageBytes((library.getUsageBytes() == null ? 0L : library.getUsageBytes()) + image.bytes().length);
        patch.setUpdatedTime(new Date());
        ShortLookLibraryExample up = new ShortLookLibraryExample();
        up.createCriteria().andIdEqualTo(library.getId());
        libraryMapper.updateByExampleSelective(patch, up);
        library.setItemCount(patch.getItemCount());
        library.setUsageBytes(patch.getUsageBytes());
        return row;
    }

    private ShortLookLibrary defaultLibrary(String userId) {
        for (ShortLookLibrary library : librariesOf(userId)) {
            if ("ACTIVE".equals(library.getStatus()) && library.getMountPortrait() != null && library.getMountPortrait() == 1) {
                return library;
            }
        }
        ShortLookLibrary row = new ShortLookLibrary();
        row.setLibraryId(ShortIds.id("lk-"));
        row.setUserId(userId);
        row.setLibraryName(DEFAULT_NAME);
        row.setDescription("定妆时优先参考这里的图片");
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
        for (ShortLookMaterial material : materials) {
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("material_id", material.getMaterialId());
            file.put("material_name", material.getMaterialName());
            file.put("caption", material.getCaption());
            file.put("material_kind", material.getMaterialKind());
            file.put("view_angle", material.getViewAngle());
            file.put("view_name", material.getViewAngle() == null ? null : viewName(material.getViewAngle()));
            file.put("file_url", presign(material.getStorageKey()));
            file.put("width", material.getWidth());
            file.put("height", material.getHeight());
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
