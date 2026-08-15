package com.bgssai.shortai.payment.channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;

@Component
public class PaymentChannelRouter {

    private final Map<String, PaymentChannel> channels = new HashMap<>();

    @Autowired
    public PaymentChannelRouter(List<PaymentChannel> channelList) {
        for (PaymentChannel channel : channelList) {
            channels.put(channel.channelCode(), channel);
        }
    }

    public boolean supports(String channelCode) {
        return channelCode != null && channels.containsKey(channelCode);
    }

    public PaymentChannel route(String channelCode) {
        PaymentChannel channel = channelCode == null ? null : channels.get(channelCode);
        if (channel == null) {
            throw new BizException("60004", "unsupported pay channel: " + channelCode);
        }
        return channel;
    }
}
