package com.bgssai.shortai.loginlog.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.ShanghaiClock;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.loginlog.dto.LoginLogItem;
import com.bgssai.shortai.mapper.LoginLogMapper;
import com.bgssai.shortai.model.LoginLog;
import com.bgssai.shortai.model.LoginLogExample;

import com.github.pagehelper.PageHelper;

/**
 * List the current user's own login logs, newest first, physically paginated.
 * The user id comes from UserContext (populated by JwtTokenAspect via @NeedAop) — never from a
 * request parameter, so one user can never read another's logs.
 * Reference for the mandated persistence pattern (Standards §2): only Generator Example methods
 * are called, and paging goes through PageHelper. The whole path reads top-to-bottom in one
 * method (Standards §1 Flat Code).
 */
@Service
public class ListLoginLogsService {

    @Autowired
    private LoginLogMapper loginLogMapper;

    public PageResponse list(int pageNum, int pageSize) {
        String currentUserId = UserContext.getUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;

        LoginLogExample example = new LoginLogExample();
        example.createCriteria()
                .andDelFlagEqualTo(Boolean.FALSE)
                .andUserIdEqualTo(currentUserId);
        example.setOrderByClause("create_time DESC, id DESC");

        PageHelper.startPage(safePageNum, safePageSize);
        List<LoginLog> rows = loginLogMapper.selectByExample(example);
        long total = ((com.github.pagehelper.Page<LoginLog>) rows).getTotal();

        List<LoginLogItem> items = new ArrayList<>(rows.size());
        for (LoginLog row : rows) {
            LoginLogItem item = new LoginLogItem();
            item.setId(row.getId());
            item.setUserId(row.getUserId());
            item.setAction(row.getAction());
            item.setIpAddress(row.getIpAddress());
            item.setUserAgent(row.getUserAgent());
            item.setCreateTime(ShanghaiClock.format(row.getCreateTime()));
            items.add(item);
        }

        PageResponse response = new PageResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setResult(items);
        response.setPageNum(safePageNum);
        response.setPageSize(safePageSize);
        response.setTotalSize(total);
        return response;
    }
}
