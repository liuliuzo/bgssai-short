package com.bgssai.shortai.supportbridge;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bgssai.shortai.mapper.ShortCsMessageMapper;
import com.bgssai.shortai.mapper.ShortCsSessionMapper;
import com.bgssai.shortai.mapper.ShortCsTicketMapper;
import com.bgssai.shortai.model.ShortCsMessage;
import com.bgssai.shortai.model.ShortCsSession;
import com.bgssai.shortai.model.ShortCsSessionExample;
import com.bgssai.shortai.model.ShortCsTicket;
import com.bgssai.shortai.model.ShortCsTicketExample;

/** 统一客服回写到短剧站内客服表。不触碰模型调用。 */
@Service
public class ShortSupportInboxWriter implements SupportInboxWriter {

    private static final int BODY_MAX = 2000;
    private static final Map<String, String> TICKET_STATUS = Map.of(
            "OPEN", "OPEN",
            "IN_PROGRESS", "OPEN",
            "PENDING", "OPEN",
            "RESOLVED", "CLOSED",
            "CLOSED", "CLOSED");

    private final ShortCsSessionMapper sessionMapper;
    private final ShortCsMessageMapper messageMapper;
    private final ShortCsTicketMapper ticketMapper;

    public ShortSupportInboxWriter(ShortCsSessionMapper sessionMapper, ShortCsMessageMapper messageMapper,
                                    ShortCsTicketMapper ticketMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public void appendAgentReply(String externalSessionId, String body) {
        writeReply(sessionOf(externalSessionId), body);
    }

    @Override
    public void appendTicketReply(String externalTicketId, String externalSessionId, String body) {
        writeReply(sessionForTicket(externalTicketId, externalSessionId), body);
    }

    @Override
    public void updateSessionStatus(String externalSessionId, String status) {
        String mapped = switch (status == null ? "" : status.trim()) {
            case "open" -> "OPEN";
            case "pending" -> "PENDING";
            case "closed" -> "CLOSED";
            default -> throw new SupportBridgeException(400, "无法识别的会话状态");
        };
        ShortCsSession session = sessionOf(externalSessionId);
        ShortCsSession patch = new ShortCsSession();
        patch.setId(session.getId());
        patch.setStatus(mapped);
        patch.setUpdateBy("support");
        patch.setUpdateTime(new Date());
        if (sessionMapper.updateByPrimaryKeySelective(patch) != 1) {
            throw new SupportBridgeException(500, "更新客服会话失败");
        }
    }

    @Override
    public void updateTicketStatus(String externalTicketId, String externalSessionId, String status) {
        ShortCsTicket ticket = ticketOf(externalTicketId);
        if (externalSessionId != null && !externalSessionId.isBlank()
                && !externalSessionId.equals(ticket.getSessionId())) {
            throw new SupportBridgeException(422, "工单与会话不匹配");
        }
        String key = status == null ? "" : status.trim().toUpperCase();
        String mapped = TICKET_STATUS.get(key);
        if (mapped == null) {
            throw new SupportBridgeException(400, "无法识别的工单状态");
        }
        ShortCsTicket patch = new ShortCsTicket();
        patch.setId(ticket.getId());
        patch.setStatus(mapped);
        patch.setUpdateBy("support");
        patch.setUpdateTime(new Date());
        if (ticketMapper.updateByPrimaryKeySelective(patch) != 1) {
            throw new SupportBridgeException(500, "更新客服工单失败");
        }
    }

    private void writeReply(ShortCsSession session, String body) {
        if (body == null || body.isBlank()) {
            throw new SupportBridgeException(400, "客服回复为空");
        }
        if (body.length() > BODY_MAX) {
            throw new SupportBridgeException(400, "客服回复超过长度限制");
        }
        Date now = new Date();
        ShortCsMessage message = new ShortCsMessage();
        message.setMessageId("cm-" + UUID.randomUUID().toString().replace("-", ""));
        message.setSessionId(session.getSessionId());
        message.setSenderType("ADMIN");
        message.setSenderId("support");
        message.setContent(body);
        message.setDelFlag(Boolean.FALSE);
        message.setCreateBy("support");
        message.setUpdateBy("support");
        message.setCreateTime(now);
        message.setUpdateTime(now);
        if (messageMapper.insertSelective(message) != 1) {
            throw new SupportBridgeException(500, "写入客服回复失败");
        }
        int unread = session.getUnreadUser() == null ? 0 : session.getUnreadUser();
        ShortCsSession patch = new ShortCsSession();
        patch.setId(session.getId());
        patch.setStatus("OPEN");
        patch.setLastMessageAt(now);
        patch.setUnreadUser(unread + 1);
        patch.setUpdateBy("support");
        patch.setUpdateTime(now);
        if (sessionMapper.updateByPrimaryKeySelective(patch) != 1) {
            throw new SupportBridgeException(500, "更新客服会话失败");
        }
    }

    private ShortCsSession sessionForTicket(String externalTicketId, String externalSessionId) {
        if (externalTicketId != null && !externalTicketId.isBlank()) {
            ShortCsTicket ticket = ticketOf(externalTicketId);
            if (externalSessionId != null && !externalSessionId.isBlank()
                    && !externalSessionId.equals(ticket.getSessionId())) {
                throw new SupportBridgeException(422, "工单与会话不匹配");
            }
            return sessionOf(ticket.getSessionId());
        }
        return sessionOf(externalSessionId);
    }

    private ShortCsSession sessionOf(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new SupportBridgeException(422, "无法对应客服会话");
        }
        ShortCsSessionExample ex = new ShortCsSessionExample();
        ex.createCriteria().andSessionIdEqualTo(externalId.trim()).andDelFlagEqualTo(Boolean.FALSE);
        ShortCsSession row = sessionMapper.selectOneByExample(ex);
        if (row == null) {
            throw new SupportBridgeException(422, "无法对应客服会话");
        }
        return row;
    }

    private ShortCsTicket ticketOf(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new SupportBridgeException(422, "无法对应客服工单");
        }
        ShortCsTicketExample ex = new ShortCsTicketExample();
        ex.createCriteria().andTicketIdEqualTo(externalId.trim()).andDelFlagEqualTo(Boolean.FALSE);
        List<ShortCsTicket> rows = ticketMapper.selectByExample(ex);
        if (rows == null || rows.isEmpty()) {
            throw new SupportBridgeException(422, "无法对应客服工单");
        }
        return rows.get(0);
    }
}
