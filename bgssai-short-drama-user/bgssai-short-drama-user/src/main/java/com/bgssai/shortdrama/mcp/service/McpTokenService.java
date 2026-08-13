package com.bgssai.shortdrama.mcp.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortdrama.common.aop.UserContext;
import com.bgssai.shortdrama.common.exception.BizException;
import com.bgssai.shortdrama.common.exception.CommonErrorCode;
import com.bgssai.shortdrama.mapper.McpAccessTokenMapper;
import com.bgssai.shortdrama.mcp.domain.McpCaller;
import com.bgssai.shortdrama.model.McpAccessToken;
import com.bgssai.shortdrama.model.McpAccessTokenExample;

@Service
public class McpTokenService {

    private static final Logger log = LoggerFactory.getLogger(McpTokenService.class);

    public static final String TOKEN_PREFIX = "bgs_mcp_";
    private static final int MAX_TOKENS_PER_USER = 5;
    private static final int NAME_MAX_LEN = 64;
    private static final long LAST_USED_WRITE_INTERVAL_MS = 60_000L;

    private final McpAccessTokenMapper tokenMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public McpTokenService(McpAccessTokenMapper tokenMapper) {
        this.tokenMapper = tokenMapper;
    }

    @Transactional
    public Map<String, Object> create(String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "令牌名称不能为空");
        }
        if (cleanName.length() > NAME_MAX_LEN) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "令牌名称不得超过 " + NAME_MAX_LEN + " 字");
        }
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "请先登录");
        }

        McpAccessTokenExample countEx = new McpAccessTokenExample();
        countEx.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(Boolean.FALSE);
        if (tokenMapper.countByExample(countEx) >= MAX_TOKENS_PER_USER) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "最多创建 " + MAX_TOKENS_PER_USER + " 枚 MCP 令牌，请先吊销不用的");
        }

        byte[] rand = new byte[20];
        secureRandom.nextBytes(rand);
        String plaintext = TOKEN_PREFIX + HexFormat.of().formatHex(rand);

        Date now = new Date();
        McpAccessToken row = new McpAccessToken();
        row.setUserId(userId);
        row.setTokenName(cleanName);
        row.setTokenHash(plaintext);
        row.setDelFlag(Boolean.FALSE);
        row.setCreateBy(userId);
        row.setUpdateBy(userId);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        tokenMapper.insertSelective(row);
        log.info("createMcpToken userId={} tokenId={} name={}", userId, row.getId(), cleanName);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", row.getId());
        out.put("token_name", cleanName);
        out.put("token", plaintext);
        return out;
    }

    public List<Map<String, Object>> listMine() {
        String userId = UserContext.getUserId();
        McpAccessTokenExample ex = new McpAccessTokenExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(Boolean.FALSE);
        ex.setOrderByClause("id DESC");
        List<McpAccessToken> rows = tokenMapper.selectByExample(ex);
        List<Map<String, Object>> items = new ArrayList<>();
        if (rows == null) {
            return items;
        }
        for (McpAccessToken r : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("token_name", r.getTokenName());
            item.put("token_masked", TOKEN_PREFIX + "****");
            item.put("last_used_time", r.getLastUsedTime());
            item.put("create_time", r.getCreateTime());
            items.add(item);
        }
        return items;
    }

    @Transactional
    public void revoke(Long tokenId) {
        String userId = UserContext.getUserId();
        if (tokenId == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "缺少令牌 id");
        }
        McpAccessToken row = tokenMapper.selectByPrimaryKey(tokenId);
        if (row == null || Boolean.TRUE.equals(row.getDelFlag()) || !userId.equals(row.getUserId())) {
            throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "令牌不存在");
        }
        McpAccessToken patch = new McpAccessToken();
        patch.setId(tokenId);
        patch.setDelFlag(Boolean.TRUE);
        patch.setUpdateBy(userId);
        patch.setUpdateTime(new Date());
        tokenMapper.updateByPrimaryKeySelective(patch);
        log.info("revokeMcpToken userId={} tokenId={}", userId, tokenId);
    }

    public McpCaller verify(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        McpAccessTokenExample ex = new McpAccessTokenExample();
        ex.createCriteria().andTokenHashEqualTo(rawToken).andDelFlagEqualTo(Boolean.FALSE);
        McpAccessToken row = tokenMapper.selectOneByExample(ex);
        if (row == null) {
            return null;
        }
        Date now = new Date();
        if (row.getLastUsedTime() == null
                || now.getTime() - row.getLastUsedTime().getTime() > LAST_USED_WRITE_INTERVAL_MS) {
            McpAccessToken patch = new McpAccessToken();
            patch.setId(row.getId());
            patch.setLastUsedTime(now);
            patch.setUpdateTime(now);
            tokenMapper.updateByPrimaryKeySelective(patch);
        }
        return new McpCaller(row.getUserId());
    }
}
