package com.bgssai.shortai.usage.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.AiUsageRecordMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.AiUsageRecord;
import com.bgssai.shortai.model.AiUsageRecordExample;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;

@Service
public class UsageAdminService {

    private static final ZoneId SHANGHAI = UsageCostMath.SHANGHAI;

    @Autowired
    private AiUsageRecordMapper aiUsageRecordMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    public ApiResponse summary(String startDate, String endDate, String abilityType) {
        requireAdmin();
        DateRange range = parseRange(startDate, endDate);
        List<ShortAiTask> tasks = tasksIn(range, abilityType);
        List<AiUsageRecord> usageRows = usageIn(range, abilityType);

        int totalCost = 0;
        int succeeded = 0;
        long durationSum = 0;
        int durationCount = 0;
        Map<String, int[]> byAbility = new LinkedHashMap<>();
        Map<String, int[]> byDay = new LinkedHashMap<>();
        Set<String> rolledTaskIds = new HashSet<>();

        for (ShortAiTask task : tasks) {
            if (task.getTaskId() != null) {
                rolledTaskIds.add(task.getTaskId());
            }
            if ("SUCCEEDED".equals(task.getTaskStatus())) {
                succeeded++;
            }
            int billed = UsageCostMath.billedCent(task.getTaskStatus(), task.getCostCent());
            totalCost += billed;
            String ability = task.getTaskType() == null ? "UNKNOWN" : task.getTaskType();
            addBucket(byAbility, ability, billed, 1);
            addBucket(byDay, UsageCostMath.shanghaiDay(task.getCreatedTime()), billed, 1);
            Long seconds = UsageCostMath.generationSeconds(task.getStartedTime(), task.getFinishedTime());
            if (seconds != null) {
                durationSum += seconds;
                durationCount++;
            }
        }
        for (AiUsageRecord row : usageRows) {
            if (row.getTaskId() != null && rolledTaskIds.contains(row.getTaskId())) {
                continue;
            }
            int cost = row.getCostCent() == null ? 0 : Math.max(0, row.getCostCent());
            totalCost += cost;
            String ability = row.getAbilityType() == null ? "UNKNOWN" : row.getAbilityType();
            addBucket(byAbility, ability, cost, 1);
            addBucket(byDay, UsageCostMath.shanghaiDay(row.getCreatedTime()), cost, 1);
        }

        double successRate = tasks.isEmpty() ? 0.0 : (succeeded * 1000L / tasks.size()) / 10.0;
        double avgDuration = durationCount == 0 ? 0.0 : Math.round((durationSum * 10.0) / durationCount) / 10.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_cost_cent", totalCost);
        result.put("task_count", tasks.size());
        result.put("success_rate_percent", successRate);
        result.put("avg_duration_sec", avgDuration);
        result.put("by_ability_list", abilityList(byAbility));
        result.put("by_day_list", dayList(byDay));
        return Responses.ok(result);
    }

    public PageResponse byCreator(int pageNum, int pageSize, String startDate, String endDate) {
        requireAdmin();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        DateRange range = parseRange(startDate, endDate);
        List<ShortAiTask> tasks = tasksIn(range, null);
        List<AiUsageRecord> usageRows = usageIn(range, null);
        Map<String, int[]> agg = new LinkedHashMap<>();
        Set<String> rolledTaskIds = new HashSet<>();
        for (ShortAiTask task : tasks) {
            if (task.getTaskId() != null) {
                rolledTaskIds.add(task.getTaskId());
            }
            String uid = task.getUserId() == null ? "" : task.getUserId();
            addBucket(agg, uid, UsageCostMath.billedCent(task.getTaskStatus(), task.getCostCent()), 1);
        }
        for (AiUsageRecord row : usageRows) {
            if (row.getTaskId() != null && rolledTaskIds.contains(row.getTaskId())) {
                continue;
            }
            String uid = row.getUserId() == null ? "" : row.getUserId();
            int cost = row.getCostCent() == null ? 0 : Math.max(0, row.getCostCent());
            addBucket(agg, uid, cost, 1);
        }
        List<Map.Entry<String, int[]>> entries = new ArrayList<>(agg.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
        long total = entries.size();
        int from = Math.min((safePageNum - 1) * safePageSize, entries.size());
        int to = Math.min(from + safePageSize, entries.size());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, int[]> e : entries.subList(from, to)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("user_id", e.getKey());
            item.put("cost_cent", e.getValue()[0]);
            item.put("task_count", e.getValue()[1]);
            SysUserExample ux = new SysUserExample();
            ux.createCriteria().andUserIdEqualTo(e.getKey());
            SysUser user = sysUserMapper.selectOneByExample(ux);
            item.put("user_name", user == null ? null : user.getUserName());
            item.put("real_name", user == null ? null : user.getRealName());
            items.add(item);
        }
        PageResponse response = new PageResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setMessage("OK");
        response.setResult(items);
        response.setPageNum(safePageNum);
        response.setPageSize(safePageSize);
        response.setTotalSize(total);
        return response;
    }

    private List<ShortAiTask> tasksIn(DateRange range, String abilityType) {
        ShortAiTaskExample tex = new ShortAiTaskExample();
        ShortAiTaskExample.Criteria tc = tex.createCriteria();
        tc.andCreatedTimeGreaterThan(range.startExclusive);
        tc.andCreatedTimeLessThan(range.endExclusive);
        if (abilityType != null && !abilityType.isBlank()) {
            tc.andTaskTypeEqualTo(abilityType.trim());
        }
        return shortAiTaskMapper.selectByExample(tex);
    }

    private List<AiUsageRecord> usageIn(DateRange range, String abilityType) {
        AiUsageRecordExample ex = new AiUsageRecordExample();
        AiUsageRecordExample.Criteria c = ex.createCriteria();
        c.andCreatedTimeGreaterThan(range.startExclusive);
        c.andCreatedTimeLessThan(range.endExclusive);
        if (abilityType != null && !abilityType.isBlank()) {
            c.andAbilityTypeEqualTo(abilityType.trim());
        }
        return aiUsageRecordMapper.selectByExample(ex);
    }

    private static void addBucket(Map<String, int[]> buckets, String key, int cost, int count) {
        int[] bucket = buckets.computeIfAbsent(key == null ? "" : key, k -> new int[2]);
        bucket[0] += cost;
        bucket[1] += count;
    }

    private static List<Map<String, Object>> abilityList(Map<String, int[]> byAbility) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byAbility.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ability_type", e.getKey());
            item.put("ability_type_name", abilityName(e.getKey()));
            item.put("cost_cent", e.getValue()[0]);
            item.put("task_count", e.getValue()[1]);
            list.add(item);
        }
        return list;
    }

    private static List<Map<String, Object>> dayList(Map<String, int[]> byDay) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byDay.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", e.getKey());
            item.put("cost_cent", e.getValue()[0]);
            item.put("task_count", e.getValue()[1]);
            list.add(item);
        }
        return list;
    }

    static String abilityName(String code) {
        if (code == null) {
            return "未知";
        }
        return switch (code) {
            case "TEXT" -> "文本";
            case "IMAGE" -> "图片";
            case "VIDEO" -> "视频";
            case "VOICE" -> "配音";
            case "ASR" -> "语音识别";
            case "MUSIC" -> "音乐生成";
            case "COMPOSE" -> "合成";
            default -> code;
        };
    }

    private static DateRange parseRange(String startDate, String endDate) {
        ZonedDateTime endExclusive;
        if (endDate == null || endDate.isBlank()) {
            endExclusive = LocalDate.now(SHANGHAI).plusDays(1).atStartOfDay(SHANGHAI);
        } else {
            endExclusive = dayStart(endDate).plusDays(1);
        }
        ZonedDateTime startExclusive;
        if (startDate == null || startDate.isBlank()) {
            startExclusive = endExclusive.minusDays(31).minusNanos(1_000_000);
        } else {
            startExclusive = dayStart(startDate).minusNanos(1_000_000);
        }
        return new DateRange(Date.from(startExclusive.toInstant()), Date.from(endExclusive.toInstant()));
    }

    private static ZonedDateTime dayStart(String ymd) {
        try {
            return LocalDate.parse(ymd.trim()).atStartOfDay(SHANGHAI);
        } catch (Exception ex) {
            return LocalDate.now(SHANGHAI).atStartOfDay(SHANGHAI);
        }
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can view usage");
        }
    }

    private record DateRange(Date startExclusive, Date endExclusive) {
    }
}
