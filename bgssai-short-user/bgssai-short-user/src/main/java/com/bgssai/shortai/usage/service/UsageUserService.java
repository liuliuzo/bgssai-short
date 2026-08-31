package com.bgssai.shortai.usage.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.mapper.AiUsageRecordMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.AiUsageRecord;
import com.bgssai.shortai.model.AiUsageRecordExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.quota.service.QuotaWalletService;
import com.github.pagehelper.PageHelper;

@Service
public class UsageUserService {

    private static final Map<String, String> ABILITY_NAME = Map.of(
            "TEXT", "文本",
            "IMAGE", "图像",
            "VIDEO", "视频",
            "VOICE", "配音",
            "ASR", "语音识别",
            "MUSIC", "音乐生成",
            "COMPOSE", "合成");
    private static final Map<String, String> QUALITY_NAME = Map.of(
            "FAST", "快速",
            "STANDARD", "标准",
            "HIGH", "高质");

    @Autowired
    private AiUsageRecordMapper aiUsageRecordMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private QuotaWalletService quotaWalletService;

    public ApiResponse summary(String startDate, String endDate) {
        String userId = requireUser();
        UserQuota quota = quotaWalletService.requireWallet(userId);
        quotaWalletService.resetMonthIfNeeded(quota);
        int balance = n(quota.getBalanceCent());
        int extra = n(quota.getExtraBalanceCent());
        int available = balance + extra;

        DateRange range = parseRange(startDate, endDate);
        AiUsageRecordExample ex = new AiUsageRecordExample();
        ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andCreatedTimeGreaterThan(range.startExclusive)
                .andCreatedTimeLessThan(range.endExclusive);
        List<AiUsageRecord> rows = aiUsageRecordMapper.selectByExample(ex);

        Map<String, int[]> byWork = new LinkedHashMap<>();
        Map<String, int[]> byAbility = new LinkedHashMap<>();
        Map<String, Integer> episodeCost = new LinkedHashMap<>();
        for (AiUsageRecord row : rows) {
            int cost = n(row.getCostCent());
            String workId = row.getWorkId() == null ? "" : row.getWorkId();
            int[] w = byWork.computeIfAbsent(workId, k -> new int[2]);
            w[0] += cost;
            w[1] += 1;
            String ability = row.getAbilityType() == null ? "UNKNOWN" : row.getAbilityType();
            int[] a = byAbility.computeIfAbsent(ability, k -> new int[2]);
            a[0] += cost;
            a[1] += 1;
            if (row.getEpisodeId() != null && !row.getEpisodeId().isBlank()) {
                episodeCost.merge(row.getEpisodeId(), cost, Integer::sum);
            }
        }

        int recentAvg = 0;
        if (!episodeCost.isEmpty()) {
            long sum = 0;
            for (Integer c : episodeCost.values()) {
                sum += c;
            }
            recentAvg = (int) (sum / episodeCost.size());
        }
        int remaining = recentAvg <= 0 ? 0 : available / recentAvg;
        int lowBalance = (recentAvg > 0 && available < recentAvg * 3L) ? 1 : 0;

        Map<String, String> workTitles = loadWorkTitles(byWork.keySet());
        List<Map<String, Object>> byWorkList = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byWork.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("work_id", e.getKey());
            item.put("work_title", workTitles.get(e.getKey()));
            item.put("cost_cent", e.getValue()[0]);
            item.put("task_count", e.getValue()[1]);
            byWorkList.add(item);
        }
        List<Map<String, Object>> byAbilityList = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byAbility.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ability_type", e.getKey());
            item.put("ability_type_name", ABILITY_NAME.getOrDefault(e.getKey(), e.getKey()));
            item.put("cost_cent", e.getValue()[0]);
            item.put("task_count", e.getValue()[1]);
            byAbilityList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance_cent", balance);
        result.put("extra_balance_cent", extra);
        result.put("available_cent", available);
        result.put("monthly_limit_cent", n(quota.getMonthlyLimitCent()));
        result.put("month_spent_cent", n(quota.getMonthSpentCent()));
        result.put("recent_episode_avg_cost_cent", recentAvg);
        result.put("estimated_remaining_episode_count", remaining);
        result.put("low_balance_flag", lowBalance);
        result.put("by_work_list", byWorkList);
        result.put("by_ability_list", byAbilityList);
        return Responses.ok(result);
    }

    public ApiResponse records(int pageNum, int pageSize, String workId, String episodeId, String abilityType,
            String startDate, String endDate) {
        String userId = requireUser();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        DateRange range = parseRange(startDate, endDate);
        AiUsageRecordExample ex = new AiUsageRecordExample();
        AiUsageRecordExample.Criteria c = ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andCreatedTimeGreaterThan(range.startExclusive)
                .andCreatedTimeLessThan(range.endExclusive);
        if (workId != null && !workId.isBlank()) {
            c.andWorkIdEqualTo(workId.trim());
        }
        if (episodeId != null && !episodeId.isBlank()) {
            c.andEpisodeIdEqualTo(episodeId.trim());
        }
        if (abilityType != null && !abilityType.isBlank()) {
            c.andAbilityTypeEqualTo(abilityType.trim());
        }
        ex.setOrderByClause("created_time desc, id desc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<AiUsageRecord> rows = aiUsageRecordMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<AiUsageRecord>) rows).getTotal();

        Map<String, String> workTitles = new HashMap<>();
        Map<String, Integer> episodeNos = new HashMap<>();
        Map<String, Integer> shotNos = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (AiUsageRecord row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("record_id", row.getRecordId());
            item.put("ability_type", row.getAbilityType());
            item.put("ability_type_name", ABILITY_NAME.getOrDefault(row.getAbilityType(), row.getAbilityType()));
            item.put("model_code", row.getModelCode());
            item.put("quality_level", row.getQualityLevel());
            item.put("quality_level_name", QUALITY_NAME.getOrDefault(row.getQualityLevel(), row.getQualityLevel()));
            item.put("price_unit", row.getPriceUnit());
            item.put("usage_amount", row.getUsageAmount());
            item.put("unit_price_cent", row.getUnitPriceCent());
            item.put("cost_cent", row.getCostCent());
            item.put("work_id", row.getWorkId());
            item.put("work_title", resolveWorkTitle(workTitles, row.getWorkId()));
            item.put("episode_id", row.getEpisodeId());
            item.put("episode_no", resolveEpisodeNo(episodeNos, row.getEpisodeId()));
            item.put("shot_id", row.getShotId());
            item.put("shot_no", resolveShotNo(shotNos, row.getShotId()));
            item.put("task_id", row.getTaskId());
            item.put("created_time", row.getCreatedTime() == null ? null : fmt.format(row.getCreatedTime()));
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("record_list", list);
        if (episodeId != null && !episodeId.isBlank()) {
            result.put("episode_total_cost_cent", sumEpisodeCost(userId, episodeId.trim()));
        }
        result.put("page_num", safePageNum);
        result.put("page_size", safePageSize);
        result.put("total_size", total);
        return Responses.ok(result);
    }

    private int sumEpisodeCost(String userId, String episodeId) {
        AiUsageRecordExample ex = new AiUsageRecordExample();
        ex.createCriteria().andUserIdEqualTo(userId).andEpisodeIdEqualTo(episodeId);
        List<AiUsageRecord> rows = aiUsageRecordMapper.selectByExample(ex);
        int sum = 0;
        for (AiUsageRecord row : rows) {
            sum += n(row.getCostCent());
        }
        return sum;
    }

    private Map<String, String> loadWorkTitles(Iterable<String> workIds) {
        Map<String, String> map = new HashMap<>();
        for (String workId : workIds) {
            if (workId == null || workId.isBlank()) {
                continue;
            }
            map.put(workId, resolveWorkTitle(map, workId));
        }
        return map;
    }

    private String resolveWorkTitle(Map<String, String> cache, String workId) {
        if (workId == null || workId.isBlank()) {
            return null;
        }
        if (cache.containsKey(workId)) {
            return cache.get(workId);
        }
        ShortWorkExample ex = new ShortWorkExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(ex);
        String title = work == null ? null : work.getWorkTitle();
        cache.put(workId, title);
        return title;
    }

    private Integer resolveEpisodeNo(Map<String, Integer> cache, String episodeId) {
        if (episodeId == null || episodeId.isBlank()) {
            return null;
        }
        if (cache.containsKey(episodeId)) {
            return cache.get(episodeId);
        }
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ShortEpisode ep = shortEpisodeMapper.selectOneByExample(ex);
        Integer no = ep == null ? null : ep.getEpisodeNo();
        cache.put(episodeId, no);
        return no;
    }

    private Integer resolveShotNo(Map<String, Integer> cache, String shotId) {
        if (shotId == null || shotId.isBlank()) {
            return null;
        }
        if (cache.containsKey(shotId)) {
            return cache.get(shotId);
        }
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        Integer no = shot == null ? null : shot.getShotNo();
        cache.put(shotId, no);
        return no;
    }

    private static DateRange parseRange(String startDate, String endDate) {
        Calendar cal = Calendar.getInstance();
        Date endExclusive;
        if (endDate == null || endDate.isBlank()) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DATE, 1);
            endExclusive = cal.getTime();
        } else {
            endExclusive = dayStart(endDate);
            cal.setTime(endExclusive);
            cal.add(Calendar.DATE, 1);
            endExclusive = cal.getTime();
        }
        Date startExclusive;
        if (startDate == null || startDate.isBlank()) {
            cal.setTime(endExclusive);
            cal.add(Calendar.DATE, -31);
            startExclusive = new Date(cal.getTimeInMillis() - 1);
        } else {
            startExclusive = new Date(dayStart(startDate).getTime() - 1);
        }
        return new DateRange(startExclusive, endExclusive);
    }

    private static Date dayStart(String ymd) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(ymd.trim());
        } catch (Exception ex) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        }
    }

    private static String requireUser() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        return userId;
    }

    private static int n(Integer v) {
        return v == null ? 0 : v;
    }

    private record DateRange(Date startExclusive, Date endExclusive) {
    }
}
