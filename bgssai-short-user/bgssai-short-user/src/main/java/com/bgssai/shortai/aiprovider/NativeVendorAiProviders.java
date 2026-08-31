package com.bgssai.shortai.aiprovider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bgssai.shortai.secret.SysSecretReader;

/**
 * 原厂 OpenAI 兼容供应商。每个 Bean 一个 provider_code，密钥走 sys_secret。
 */
@Configuration
public class NativeVendorAiProviders {

    @Bean
    public AiProvider hunyuanNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("HUNYUAN", SysSecretReader.AI_HUNYUAN_API_KEY, secrets);
    }

    @Bean
    public AiProvider deepseekNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("DEEPSEEK", SysSecretReader.AI_DEEPSEEK_API_KEY, secrets);
    }

    @Bean
    public AiProvider zhipuNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("ZHIPU", SysSecretReader.AI_ZHIPU_API_KEY, secrets);
    }

    @Bean
    public AiProvider moonshotNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("MOONSHOT", SysSecretReader.AI_MOONSHOT_API_KEY, secrets);
    }

    @Bean
    public AiProvider dashscopeNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("DASHSCOPE", SysSecretReader.AI_DASHSCOPE_API_KEY, secrets);
    }

    @Bean
    public AiProvider spacexaiNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("SPACEXAI", SysSecretReader.AI_XAI_API_KEY, secrets);
    }

    @Bean
    public AiProvider openaiNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("OPENAI", SysSecretReader.AI_OPENAI_COMPAT_API_KEY, secrets);
    }

    @Bean
    public AiProvider xiaomiNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("XIAOMI", SysSecretReader.AI_XIAOMI_API_KEY, secrets);
    }

    @Bean
    public AiProvider volcengineNativeProvider(SysSecretReader secrets) {
        return new OpenAiCompatibleAiProvider("VOLCENGINE", SysSecretReader.AI_VOLCENGINE_API_KEY, secrets);
    }
}
