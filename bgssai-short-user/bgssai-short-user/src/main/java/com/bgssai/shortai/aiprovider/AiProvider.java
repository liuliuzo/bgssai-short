package com.bgssai.shortai.aiprovider;

public interface AiProvider {

    String providerCode();

    AiProviderResult execute(AiProviderRequest request);
}
