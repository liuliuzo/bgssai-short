package com.bgssai.shortai.login.dto.resp;

public class RespCodeSend {
    private Integer expiresIn;
    /** 短信发码写入模板的产品名；邮箱发码为空。 */
    private String product;
    /** 短信发码递增序号；邮箱发码为空。 */
    private Long seq;

    public Integer getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public Long getSeq() { return seq; }
    public void setSeq(Long seq) { this.seq = seq; }
}
