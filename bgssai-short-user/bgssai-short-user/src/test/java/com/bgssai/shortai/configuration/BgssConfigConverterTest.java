package com.bgssai.shortai.configuration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

class BgssConfigConverterTest {

    /** 支付回调要按原文验签：JSON 回调体读成 String 时必须由字符串转换器先接手。 */
    @Test
    void stringConverterPrecedesJacksonSoRawJsonBodiesAreReadable() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        new BgssConfig().extendMessageConverters(converters);
        assertInstanceOf(StringHttpMessageConverter.class, converters.get(0));
        assertInstanceOf(MappingJackson2HttpMessageConverter.class, converters.get(1));
        assertTrue(converters.get(0).canRead(String.class, MediaType.APPLICATION_JSON));
    }
}
