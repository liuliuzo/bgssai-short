package com.bgssai.shortai.aiprovider;

public class AiProviderResult {

    private String textContent;
    private byte[] binaryContent;
    private String mimeType;
    private Integer usageAmount;
    private Integer width;
    private Integer height;
    private Integer durationSec;

    public static AiProviderResult text(String content) {
        AiProviderResult r = new AiProviderResult();
        r.textContent = content;
        r.mimeType = "application/json";
        r.usageAmount = 1;
        return r;
    }

    public static AiProviderResult binary(byte[] content, String mimeType) {
        AiProviderResult r = new AiProviderResult();
        r.binaryContent = content;
        r.mimeType = mimeType;
        r.usageAmount = 1;
        return r;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public byte[] getBinaryContent() {
        return binaryContent;
    }

    public void setBinaryContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getUsageAmount() {
        return usageAmount;
    }

    public void setUsageAmount(Integer usageAmount) {
        this.usageAmount = usageAmount;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }
}
