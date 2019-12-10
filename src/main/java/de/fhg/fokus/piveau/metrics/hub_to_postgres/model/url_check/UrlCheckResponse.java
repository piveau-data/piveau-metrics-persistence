package de.fhg.fokus.piveau.metrics.hub_to_postgres.model.url_check;

/**
 * Created by fritz on 01.06.17.
 */
public class UrlCheckResponse {

    private String url;
    private Integer statusCode;
    private String message;
    private String mimeType;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return "UrlCheckResponse{" +
                "url='" + url + '\'' +
                ", statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}
