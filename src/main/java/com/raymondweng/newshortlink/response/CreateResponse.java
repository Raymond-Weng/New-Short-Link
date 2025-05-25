package com.raymondweng.newshortlink.response;

public class CreateResponse {
    String link;
    String error;
    String short_link;
    String expiration;

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getShort_link() {
        return short_link;
    }

    public void setShort_link(String short_link) {
        this.short_link = short_link;
    }
}
