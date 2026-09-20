package com.bgssai.shortai.login.dto.resp;

import java.util.List;

/** GET /bgssai/user/oauth/channels 响应：凭证已配齐的第三方登录渠道。 */
public class RespOAuthChannels {
    private List<String> channels;

    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }
}
