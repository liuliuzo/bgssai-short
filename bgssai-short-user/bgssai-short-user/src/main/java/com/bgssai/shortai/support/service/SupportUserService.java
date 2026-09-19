package com.bgssai.shortai.support.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortCsMessageMapper;
import com.bgssai.shortai.mapper.ShortCsSessionMapper;
import com.bgssai.shortai.mapper.ShortCsTicketMapper;
import com.bgssai.shortai.model.ShortCsMessage;
import com.bgssai.shortai.model.ShortCsMessageExample;
import com.bgssai.shortai.model.ShortCsSession;
import com.bgssai.shortai.model.ShortCsSessionExample;
import com.bgssai.shortai.model.ShortCsTicket;
import com.bgssai.shortai.model.ShortCsTicketExample;
import com.bgssai.shortai.utils.JwtUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class SupportUserService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String TICKET_OPEN = "OPEN";
    public static final String TICKET_CLOSED = "CLOSED";
    public static final String SENDER_USER = "USER";
    public static final String SENDER_GUEST = "GUEST";
    public static final String SENDER_ADMIN = "ADMIN";

    private static final int CONTENT_MAX = 2000;
    private static final int SUBJECT_MAX = 200;
    private static final java.util.Set<String> TICKET_PRIORITIES = java.util.Set.of("LOW", "NORMAL", "HIGH");

    private final ShortCsSessionMapper sessionMapper;
    private final ShortCsMessageMapper messageMapper;
    private final ShortCsTicketMapper ticketMapper;

    public SupportUserService(ShortCsSessionMapper sessionMapper, ShortCsMessageMapper messageMapper,
            ShortCsTicketMapper ticketMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
    }

    public String resolveOptionalUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Jwttoken");
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            if (!JwtUtil.validateToken(token)) {
                return null;
            }
            return JwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, HttpServletRequest request) {
        String guestToken = trim(str(body, "guest_token"));
        if (!StringUtils.hasText(guestToken)) {
            guestToken = "gt-" + UUID.randomUUID().toString().replace("-", "");
        }
        String userId = resolveOptionalUserId(request);
        String contactName = trim(str(body, "contact_name"));
        String contactPhone = trim(str(body, "contact_phone"));
        String contactEmail = trim(str(body, "contact_email"));
        String content = trim(str(body, "content"));
        validateContactLens(contactName, contactPhone, contactEmail);
        if (StringUtils.hasText(content) && content.length() > CONTENT_MAX) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "留言不得超过 " + CONTENT_MAX + " 字");
        }

        Date now = new Date();
        String sessionId = "cs-" + UUID.randomUUID().toString().replace("-", "");
        ShortCsSession session = new ShortCsSession();
        session.setSessionId(sessionId);
        session.setGuestToken(guestToken);
        session.setUserId(userId);
        session.setContactName(emptyToNull(contactName));
        session.setContactPhone(emptyToNull(contactPhone));
        session.setContactEmail(emptyToNull(contactEmail));
        session.setStatus(StringUtils.hasText(content) ? STATUS_PENDING : STATUS_OPEN);
        session.setUnreadUser(0);
        session.setUnreadAdmin(StringUtils.hasText(content) ? 1 : 0);
        session.setLastMessageAt(StringUtils.hasText(content) ? now : null);
        session.setDelFlag(Boolean.FALSE);
        session.setCreateBy(userId != null ? userId : guestToken);
        session.setUpdateBy(session.getCreateBy());
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insertSelective(session);

        Map<String, Object> out = toSessionMap(session);
        out.put("guest_token", guestToken);
        if (StringUtils.hasText(content)) {
            String senderType = userId != null ? SENDER_USER : SENDER_GUEST;
            ShortCsMessage msg = insertMessage(sessionId, senderType, userId != null ? userId : guestToken, content, now);
            out.put("message", toMessageMap(msg));
        }
        return out;
    }

    public PageResponse listMine(Map<String, Object> body, HttpServletRequest request) {
        String userId = resolveOptionalUserId(request);
        String guestToken = trim(str(body, "guest_token"));
        if (!StringUtils.hasText(userId) && !StringUtils.hasText(guestToken)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "请提供 guest_token 或登录后访问");
        }
        int pageNum = intVal(body, "page_num", 1);
        int pageSize = intVal(body, "page_size", 20);
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;

        ShortCsSessionExample ex = new ShortCsSessionExample();
        ShortCsSessionExample.Criteria c = ex.createCriteria().andDelFlagEqualTo(Boolean.FALSE);
        if (StringUtils.hasText(userId)) {
            c.andUserIdEqualTo(userId);
        } else {
            c.andGuestTokenEqualTo(guestToken);
        }
        ex.setOrderByClause("last_message_at DESC, id DESC");

        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortCsSession> rows = sessionMapper.selectByExample(ex);
        long total = ((Page<ShortCsSession>) rows).getTotal();

        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortCsSession row : rows) {
            items.add(toSessionMap(row));
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
    public Map<String, Object> detail(Map<String, Object> body, HttpServletRequest request) {
        ShortCsSession session = requireOwnedSession(body, request);
        session.setUnreadUser(0);
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
    public Map<String, Object> createTicket(Map<String, Object> body, HttpServletRequest request) {
        ShortCsSession session = requireOwnedSession(body, request);
        if (STATUS_CLOSED.equals(session.getStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "会话已关闭，请新建会话后再提工单");
        }
        ShortCsTicket existing = findActiveTicket(session.getSessionId());
        if (existing != null) {
            return toTicketMap(existing);
        }

        String subject = trim(str(body, "subject"));
        if (!StringUtils.hasText(subject)) {
            subject = defaultSubjectFromMessages(session.getSessionId());
        }
        if (subject.length() > SUBJECT_MAX) {
            subject = subject.substring(0, SUBJECT_MAX);
        }
        String category = trim(str(body, "category"));
        if (category != null && category.length() > 64) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "分类过长");
        }
        String priority = trim(str(body, "priority"));
        if (!StringUtils.hasText(priority)) {
            priority = "NORMAL";
        }
        priority = priority.toUpperCase();
        if (!TICKET_PRIORITIES.contains(priority)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "priority 须为 LOW/NORMAL/HIGH");
        }
        String description = trim(str(body, "description"));
        if (description != null && description.length() > CONTENT_MAX) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "描述不得超过 " + CONTENT_MAX + " 字");
        }

        String userId = resolveOptionalUserId(request);
        String actor = userId != null ? userId : session.getGuestToken();
        Date now = new Date();
        ShortCsTicket ticket = new ShortCsTicket();
        ticket.setTicketId("ct-" + UUID.randomUUID().toString().replace("-", ""));
        ticket.setSessionId(session.getSessionId());
        ticket.setSubject(subject);
        ticket.setCategory(emptyToNull(category));
        ticket.setPriority(priority);
        ticket.setStatus(TICKET_OPEN);
        ticket.setDescription(emptyToNull(description));
        ticket.setGuestToken(session.getGuestToken());
        ticket.setUserId(StringUtils.hasText(session.getUserId()) ? session.getUserId() : userId);
        ticket.setDelFlag(Boolean.FALSE);
        ticket.setCreateBy(actor);
        ticket.setUpdateBy(actor);
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);
        ticketMapper.insertSelective(ticket);

        String senderType = userId != null ? SENDER_USER : SENDER_GUEST;
        insertMessage(session.getSessionId(), senderType, actor, "【已提工单】" + subject, now);

        session.setStatus(STATUS_PENDING);
        session.setUnreadAdmin((session.getUnreadAdmin() == null ? 0 : session.getUnreadAdmin()) + 1);
        session.setLastMessageAt(now);
        session.setUpdateBy(actor);
        session.setUpdateTime(now);
        if (userId != null && !StringUtils.hasText(session.getUserId())) {
            session.setUserId(userId);
        }
        sessionMapper.updateByPrimaryKeySelective(session);
        return toTicketMap(ticket);
    }

    @Transactional
    public Map<String, Object> send(Map<String, Object> body, HttpServletRequest request) {
        ShortCsSession session = requireOwnedSession(body, request);
        if (STATUS_CLOSED.equals(session.getStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "会话已关闭，请新建会话");
        }
        String content = trim(str(body, "content"));
        if (!StringUtils.hasText(content)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "留言内容不能为空");
        }
        if (content.length() > CONTENT_MAX) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "留言不得超过 " + CONTENT_MAX + " 字");
        }
        String userId = resolveOptionalUserId(request);
        String senderType = userId != null ? SENDER_USER : SENDER_GUEST;
        String senderId = userId != null ? userId : session.getGuestToken();
        Date now = new Date();
        ShortCsMessage msg = insertMessage(session.getSessionId(), senderType, senderId, content, now);

        session.setStatus(STATUS_PENDING);
        session.setUnreadAdmin((session.getUnreadAdmin() == null ? 0 : session.getUnreadAdmin()) + 1);
        session.setLastMessageAt(now);
        session.setUpdateBy(senderId);
        session.setUpdateTime(now);
        if (userId != null && !StringUtils.hasText(session.getUserId())) {
            session.setUserId(userId);
        }
        sessionMapper.updateByPrimaryKeySelective(session);
        return toMessageMap(msg);
    }

    private ShortCsSession requireOwnedSession(Map<String, Object> body, HttpServletRequest request) {
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
        String userId = resolveOptionalUserId(request);
        String guestToken = trim(str(body, "guest_token"));
        boolean owned = false;
        if (StringUtils.hasText(userId) && userId.equals(session.getUserId())) {
            owned = true;
        }
        if (StringUtils.hasText(guestToken) && guestToken.equals(session.getGuestToken())) {
            owned = true;
        }
        if (!owned) {
            throw new BizException(CommonErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return session;
    }

    private ShortCsTicket findActiveTicket(String sessionId) {
        ShortCsTicketExample ex = new ShortCsTicketExample();
        ex.createCriteria()
                .andSessionIdEqualTo(sessionId)
                .andDelFlagEqualTo(Boolean.FALSE)
                .andStatusNotEqualTo(TICKET_CLOSED);
        ex.setOrderByClause("id DESC");
        List<ShortCsTicket> rows = ticketMapper.selectByExample(ex);
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private ShortCsTicket findLatestTicket(String sessionId) {
        ShortCsTicketExample ex = new ShortCsTicketExample();
        ex.createCriteria().andSessionIdEqualTo(sessionId).andDelFlagEqualTo(Boolean.FALSE);
        ex.setOrderByClause("id DESC");
        List<ShortCsTicket> rows = ticketMapper.selectByExample(ex);
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private String defaultSubjectFromMessages(String sessionId) {
        ShortCsMessageExample mex = new ShortCsMessageExample();
        mex.createCriteria().andSessionIdEqualTo(sessionId).andDelFlagEqualTo(Boolean.FALSE);
        mex.setOrderByClause("create_time DESC, id DESC");
        List<ShortCsMessage> messages = messageMapper.selectByExample(mex);
        if (messages != null) {
            for (ShortCsMessage m : messages) {
                if (m == null || !StringUtils.hasText(m.getContent())) {
                    continue;
                }
                if (SENDER_USER.equals(m.getSenderType()) || SENDER_GUEST.equals(m.getSenderType())) {
                    String c = m.getContent().trim();
                    if (c.startsWith("【已提工单】")) {
                        continue;
                    }
                    return c.length() > SUBJECT_MAX ? c.substring(0, SUBJECT_MAX) : c;
                }
            }
        }
        return "客服工单";
    }

    private ShortCsMessage insertMessage(String sessionId, String senderType, String senderId, String content, Date now) {
        ShortCsMessage msg = new ShortCsMessage();
        msg.setMessageId("cm-" + UUID.randomUUID().toString().replace("-", ""));
        msg.setSessionId(sessionId);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setDelFlag(Boolean.FALSE);
        msg.setCreateBy(senderId);
        msg.setUpdateBy(senderId);
        msg.setCreateTime(now);
        msg.setUpdateTime(now);
        messageMapper.insertSelective(msg);
        return msg;
    }

    private Map<String, Object> toSessionMap(ShortCsSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("session_id", s.getSessionId());
        m.put("user_id", s.getUserId());
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
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    private static void validateContactLens(String name, String phone, String email) {
        if (name != null && name.length() > 64) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "姓名过长");
        }
        if (phone != null && phone.length() > 45) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "手机号过长");
        }
        if (email != null && email.length() > 128) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "邮箱过长");
        }
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

    private static String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
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
