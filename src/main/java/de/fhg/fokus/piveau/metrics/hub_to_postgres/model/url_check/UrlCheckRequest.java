package de.fhg.fokus.piveau.metrics.hub_to_postgres.model.url_check;

import java.util.List;

public class UrlCheckRequest {

    private List<String> urls;
    private String callback;

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    @Override
    public String toString() {
        return "UrlCheckRequest{" +
                "urls=" + urls +
                ", callback='" + callback + '\'' +
                '}';
    }
}
