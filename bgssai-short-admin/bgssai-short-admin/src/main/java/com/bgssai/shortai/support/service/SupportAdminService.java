package com.bgssai.shortai.support.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ShanghaiClock;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortCsMessageMapper;
import com.bgssai.shortai.mapper.ShortCsSessionMapper;
import com.bgssai.shortai.mapper.ShortCsTicketMapper;
import com.bgssai.shortai.model.ShortCsMessage;
import com.bgssai.shortai.model.ShortCsMessageExample;
import com.bgssai.shortai.model.ShortCsSession;
import com.bgssai.shortai.model.ShortCsSessionExample;
import com.bgssai.shortai.model.ShortCsTicket;
import com.bgssai.shortai.model.ShortCsTicketExample;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

@Service
public class SupportAdminService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String SENDER_ADMIN = "ADMIN";

    private static final int CONTENT_MAX = 2000;
    private static final java.util.Set<String> TICKET_STATUSES =
            java.util.Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");

    private final ShortCsSessionMapper sessionMapper;
    private final ShortCsMessageMapper messageMapper;
    private final ShortCsTicketMapper ticketMapper;

    public SupportAdminService(ShortCsSessionMapper sessionMapper, ShortCsMessageMapper messageMapper,
            ShortCsTicketMapper ticketMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can manage support sessions");
        }
    }

    public PageResponse list(Map<String, Object> body) {
        requireAdmin();
        int pageNum = intVal(body, "page_num", 1);
        int pageSize = intVal(body, "page_size", 20);
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        String status = trim(str(body, "status"));
        String keyword = trim(str(body, "keyword"));

        ShortCsSessionExample ex = new ShortCsSessionExample();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword + "%";
            ShortCsSessionExample.Criteria c1 = ex.createCriteria().andDelFlagEqualTo(Boolean.FALSE).andContactNameLike(like);
            if (StringUtils.hasText(status)) {
                c1.andStatusEqualTo(status);
            }
            ShortCsSessionExample.Criteria c2 = ex.or().andDelFlagEqualTo(Boolean.FALSE).andContactPhoneLike(like);
            if (StringUtils.hasText(status)) {
                c2.andStatusEqualTo(status);
            }
            ShortCsSessionExample.Criteria c3 = ex.or().andDelFlagEqualTo(Boolean.FALSE).andContactEmailLike(like);
            if (StringUtils.hasText(status)) {
                c3.andStatusEqualTo(status);
            }
            ShortCsSessionExample.Criteria c4 = ex.or().andDelFlagEqualTo(Boolean.FALSE).andSessionIdLike(like);
            if (StringUtils.hasText(status)) {
                c4.andStatusEqualTo(status);
            }
        } else {
            ShortCsSessionExample.Criteria c = ex.createCriteria().andDelFlagEqualTo(Boolean.FALSE);
            if (StringUtils.hasText(status)) {
                c.andStatusEqualTo(status);
            }
        }
        ex.setOrderByClause("unread_admin DESC, last_message_at DESC, id DESC");

        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortCsSession> rows = sessionMapper.selectByExample(ex);
        long total = ((Page<ShortCsSession>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortCsSession row : rows) {
            Map<String, Object> item = toSessionMap(row);
            ShortCsTicket ticket = findLatestTicket(row.getSessionId());
            if (ticket != null) {
                item.put("ticket_id", ticket.getTicketId());
                item.put("ticket_status", ticket.getStatus());
                item.put("ticket_subject", ticket.getSubject());
            } else {
                item.put("ticket_id", null);
                item.put("ticket_status", null);
                item.put("ticket_subject", null);
            }
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

    @Transactional
    public Map<String, Object> detail(Map<String, Object> body) {
        requireAdmin();
        ShortCsSession session = requireSession(body);
        session.setUnreadAdmin(0);
        session.setUpdateTime(new Date());
        sessionMapper.updateByPrimaryKeySelective(session);

        ShortCsMessageExample mex = new ShortCsMessageExample();
        mex.createCriteria().andSessionIdEqualTo(session.getSessionId()).andDelFlagEqualTo(Boolean.FALSE);
        mex.setOrderByClause("create_time ASC, id ASC");
        List<ShortCsMessage> messages = messageMapper.selectByExample(mex);
        List<Map<String, Object>> msgList = new ArrayList<>();
        if (messages != null) {
            for (ShortCsMessage m : messages) {
                msgList.add(toMessageMap(m));
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session", toSessionMap(session));
        out.put("messages", msgList);
        out.put("ticket", toTicketMap(findLatestTicket(session.getSessionId())));
        return out;
    }

    @Transactional
    public Map<String, Object> updateTicketStatus(Map<String, Object> body) {
        requireAdmin();
        String ticketId = trim(str(body, "ticket_id"));
        String status = trim(str(body, "status"));
        if (!StringUtils.hasText(ticketId)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "ticket_id 不能为空");
        }
        if (!StringUtils.hasText(status)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "status 不能为空");
        }
        status = status.toUpperCase();
        if (!TICKET_STATUSES.contains(status)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "status 须为 OPEN/IN_PROGRESS/RESOLVED/CLOSED");
        }
        ShortCsTicketExample ex = new ShortCsTicketExample();
        ex.createCriteria().andTicketIdEqualTo(ticketId).andDelFlagEqualTo(Boolean.FALSE);
        ShortCsTicket ticket = ticketMapper.selectOneByExample(ex);
        if (ticket == null) {
            throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "工单不存在");
        }
        String adminId = UserContext.getUserId();
        Date now = new Date();
        ticket.setStatus(status);
        ticket.setUpdateBy(adminId);
        ticket.setUpdateTime(now);
        ticketMapper.updateByPrimaryKeySelective(ticket);
        return toTicketMap(ticket);
    }

    @Transactional
    public Map<String, Object> reply(Map<String, Object> body) {
        requireAdmin();
        ShortCsSession session = requireSession(body);
        if (STATUS_CLOSED.equals(session.getStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "会话已关闭");
        }
        String content = trim(str(body, "content"));
        if (!StringUtils.hasText(content)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "回复内容不能为空");
        }
        if (content.length() > CONTENT_MAX) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "回复不得超过 " + CONTENT_MAX + " 字");
        }
        String adminId = UserContext.getUserId();
        Date now = new Date();
        ShortCsMessage msg = new ShortCsMessage();
        msg.setMessageId("cm-" + UUID.randomUUID().toString().replace("-", ""));
        msg.setSessionId(session.getSessionId());
        msg.setSenderType(SENDER_ADMIN);
        msg.setSenderId(adminId);
        msg.setContent(content);
        msg.setDelFlag(Boolean.FALSE);
        msg.setCreateBy(adminId);
        msg.setUpdateBy(adminId);
        msg.setCreateTime(now);
        msg.setUpdateTime(now);
        messageMapper.insertSelective(msg);

        session.setStatus(STATUS_OPEN);
        session.setUnreadUser((session.getUnreadUser() == null ? 0 : session.getUnreadUser()) + 1);
        session.setLastMessageAt(now);
        session.setUpdateBy(adminId);
        session.setUpdateTime(now);
        sessionMapper.updateByPrimaryKeySelective(session);
        return toMessageMap(msg);
    }

    @Transactional
    public Map<String, Object> close(Map<String, Object> body) {
        requireAdmin();
        ShortCsSession session = requireSession(body);
        String adminId = UserContext.getUserId();
        Date now = new Date();
        session.setStatus(STATUS_CLOSED);
        session.setUpdateBy(adminId);
        session.setUpdateTime(now);
        sessionMapper.updateByPrimaryKeySelective(session);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session_id", session.getSessionId());
        out.put("status", STATUS_CLOSED);
        return out;
    }

    private ShortCsSession requireSession(Map<String, Object> body) {
        String sessionId = trim(str(body, "session_id"));
        if (!StringUtils.hasText(sessionId)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "session_id 不能为空");
        }
        ShortCsSessionExample ex = new ShortCsSessionExample();
        ex.createCriteria().andSessionIdEqualTo(sessionId).andDelFlagEqualTo(Boolean.FALSE);
        ShortCsSession session = sessionMapper.selectOneByExample(ex);
        if (session == null) {
            throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "会话不存在");
        }
        return session;
    }

    private ShortCsTicket findLatestTicket(String sessionId) {
        ShortCsTicketExample ex = new ShortCsTicketExample();
        ex.createCriteria().andSessionIdEqualTo(sessionId).andDelFlagEqualTo(Boolean.FALSE);
        ex.setOrderByClause("id DESC");
        List<ShortCsTicket> rows = ticketMapper.selectByExample(ex);
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> toSessionMap(ShortCsSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("session_id", s.getSessionId());
        m.put("user_id", s.getUserId());
        m.put("guest_token", s.getGuestToken());
        m.put("contact_name", s.getContactName());
        m.put("contact_phone", s.getContactPhone());
        m.put("contact_email", s.getContactEmail());
        m.put("status", s.getStatus());
        m.put("unread_user", s.getUnreadUser());
        m.put("unread_admin", s.getUnreadAdmin());
        m.put("last_message_at", formatTime(s.getLastMessageAt()));
        m.put("create_time", formatTime(s.getCreateTime()));
        return m;
    }

    private Map<String, Object> toMessageMap(ShortCsMessage msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("message_id", msg.getMessageId());
        m.put("session_id", msg.getSessionId());
        m.put("sender_type", msg.getSenderType());
        m.put("sender_id", msg.getSenderId());
        m.put("content", msg.getContent());
        m.put("create_time", formatTime(msg.getCreateTime()));
        return m;
    }

    private Map<String, Object> toTicketMap(ShortCsTicket t) {
        if (t == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ticket_id", t.getTicketId());
        m.put("session_id", t.getSessionId());
        m.put("subject", t.getSubject());
        m.put("category", t.getCategory());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("description", t.getDescription());
        m.put("create_time", formatTime(t.getCreateTime()));
        m.put("update_time", formatTime(t.getUpdateTime()));
        return m;
    }

    private static String formatTime(Date d) {
        if (d == null) {
            return null;
        }
        return ShanghaiClock.format(d);
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        return String.valueOf(body.get(key));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static int intVal(Map<String, Object> body, String key, int def) {
        if (body == null || body.get(key) == null) {
            return def;
        }
        Object v = body.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
