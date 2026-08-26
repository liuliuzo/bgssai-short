package com.bgssai.shortai.usage.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.github.pagehelper.PageHelper;

@Service
public class UsageAdminService {

    @Autowired
    private AiUsageRecordMapper aiUsageRecordMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    public ApiResponse summary(String startDate, String endDate, String abilityType) {
        requireAdmin();
        DateRange range = parseRange(startDate, endDate);
        AiUsageRecordExample ex = new AiUsageRecordExample();
        AiUsageRecordExample.Criteria c = ex.createCriteria();
        c.andCreatedTimeGreaterThan(range.startExclusive);
        c.andCreatedTimeLessThan(range.endExclusive);
        if (abilityType != null && !abilityType.isBlank()) {
            c.andAbilityTypeEqualTo(abilityType.trim());
        }
        List<AiUsageRecord> rows = aiUsageRecordMapper.selectByExample(ex);

        int totalCost = 0;
        Map<String, int[]> byAbility = new LinkedHashMap<>();
        Map<String, int[]> byDay = new LinkedHashMap<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
        for (AiUsageRecord row : rows) {
            int cost = row.getCostCent() == null ? 0 : row.getCostCent();
            totalCost += cost;
            String ability = row.getAbilityType() == null ? "UNKNOWN" : row.getAbilityType();
            int[] a = byAbility.computeIfAbsent(ability, k -> new int[2]);
            a[0] += cost;
            a[1] += 1;
            String day = row.getCreatedTime() == null ? "" : dayFmt.format(row.getCreatedTime());
            int[] d = byDay.computeIfAbsent(day, k -> new int[2]);
            d[0] += cost;
            d[1] += 1;
        }

        ShortAiTaskExample tex = new ShortAiTaskExample();
        ShortAiTaskExample.Criteria tc = tex.createCriteria();
        tc.andCreatedTimeGreaterThan(range.startExclusive);
        tc.andCreatedTimeLessThan(range.endExclusive);
        if (abilityType != null && !abilityType.isBlank()) {
            tc.andTaskTypeEqualTo(abilityType.trim());
        }
        List<ShortAiTask> tasks = shortAiTaskMapper.selectByExample(tex);
        int succeeded = 0;
        long durationSum = 0;
        int durationCount = 0;
        for (ShortAiTask task : tasks) {
            if ("SUCCEEDED".equals(task.getTaskStatus())) {
                succeeded++;
            }
            if (task.getStartedTime() != null && task.getFinishedTime() != null) {
                durationSum += Math.max(0, task.getFinishedTime().getTime() - task.getStartedTime().getTime());
                durationCount++;
            }
        }
        double successRate = tasks.isEmpty() ? 0.0 : (succeeded * 1000L / tasks.size()) / 10.0;
        double avgDuration = durationCount == 0 ? 0.0 : Math.round((durationSum / 1000.0 / durationCount) * 10.0) / 10.0;

        List<Map<String, Object>> byAbilityList = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byAbility.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ability_type", e.getKey());
            item.put("cost_cent", e.getValue()[0]);
            item.put("task_count", e.getValue()[1]);
            byAbilityList.add(item);
        }
        List<Map<String, Object>> byDayList = new ArrayList<>();
        for (Map.Entry<String, int[]> e : byDay.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", e.getKey());
            item.put("cost_cent", e.getValue()[0]);
            item.put("task_count", e.getValue()[1]);
            byDayList.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_cost_cent", totalCost);
        result.put("task_count", tasks.size());
        result.put("success_rate_percent", successRate);
        result.put("avg_duration_sec", avgDuration);
        result.put("by_ability_list", byAbilityList);
        result.put("by_day_list", byDayList);
        return Responses.ok(result);
    }

    public PageResponse byCreator(int pageNum, int pageSize, String startDate, String endDate) {
        requireAdmin();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        DateRange range = parseRange(startDate, endDate);
        AiUsageRecordExample ex = new AiUsageRecordExample();
        ex.createCriteria()
                .andCreatedTimeGreaterThan(range.startExclusive)
                .andCreatedTimeLessThan(range.endExclusive);
        List<AiUsageRecord> rows = aiUsageRecordMapper.selectByExample(ex);
        Map<String, int[]> agg = new LinkedHashMap<>();
        for (AiUsageRecord row : rows) {
            String uid = row.getUserId() == null ? "" : row.getUserId();
            int[] a = agg.computeIfAbsent(uid, k -> new int[2]);
            a[0] += row.getCostCent() == null ? 0 : row.getCostCent();
            a[1] += 1;
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

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can view usage");
        }
    }

    private record DateRange(Date startExclusive, Date endExclusive) {
    }
}
