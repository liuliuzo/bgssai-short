package com.bgssai.shortai.loginlog.location;

/** Pieces returned by the offline IP library. Empty pieces were not in the record. */
public final class IpGeo {
    private final String display;
    private final String country;
    private final String province;
    private final String city;
    private final String isp;

    public IpGeo(String display, String country, String province, String city, String isp) {
        this.display = display;
        this.country = country;
        this.province = province;
        this.city = city;
        this.isp = isp;
    }

    public String getDisplay() { return display; }
    public String getCountry() { return country; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getIsp() { return isp; }
}
