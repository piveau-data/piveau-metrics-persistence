package de.fhg.fokus.piveau.metrics.hub_to_postgres.model;

import java.time.LocalDateTime;

public class Distribution {

    private long id;
    private String instanceId;

    private String format;
    private Boolean isMachineReadable;

    private String accessUrl;
    private Integer statusAccessUrl;
    private String accessErrorMessage;
    private LocalDateTime accessStatusLastChangeDate;

    private String downloadUrl;
    private Integer statusDownloadUrl;
    private String downloadErrorMessage;
    private LocalDateTime downloadStatusLastChangeDate;

    private String licenceId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Boolean getMachineReadable() {
        return isMachineReadable;
    }

    public void setMachineReadable(Boolean machineReadable) {
        isMachineReadable = machineReadable;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public Integer getStatusAccessUrl() {
        return statusAccessUrl;
    }

    public void setStatusAccessUrl(Integer statusAccessUrl) {
        this.statusAccessUrl = statusAccessUrl;
    }

    public String getAccessErrorMessage() {
        return accessErrorMessage;
    }

    public void setAccessErrorMessage(String accessErrorMessage) {
        this.accessErrorMessage = accessErrorMessage;
    }

    public LocalDateTime getAccessStatusLastChangeDate() {
        return accessStatusLastChangeDate;
    }

    public void setAccessStatusLastChangeDate(LocalDateTime accessStatusLastChangeDate) {
        this.accessStatusLastChangeDate = accessStatusLastChangeDate;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Integer getStatusDownloadUrl() {
        return statusDownloadUrl;
    }

    public void setStatusDownloadUrl(Integer statusDownloadUrl) {
        this.statusDownloadUrl = statusDownloadUrl;
    }

    public String getDownloadErrorMessage() {
        return downloadErrorMessage;
    }

    public void setDownloadErrorMessage(String downloadErrorMessage) {
        this.downloadErrorMessage = downloadErrorMessage;
    }

    public LocalDateTime getDownloadStatusLastChangeDate() {
        return downloadStatusLastChangeDate;
    }

    public void setDownloadStatusLastChangeDate(LocalDateTime downloadStatusLastChangeDate) {
        this.downloadStatusLastChangeDate = downloadStatusLastChangeDate;
    }

    public String getLicenceId() {
        return licenceId;
    }

    public void setLicenceId(String licenceId) {
        this.licenceId = licenceId;
    }

    @Override
    public String toString() {
        return "Distribution{" +
                "id=" + id +
                ", instanceId='" + instanceId + '\'' +
                ", format='" + format + '\'' +
                ", accessUrl='" + accessUrl + '\'' +
                ", statusAccessUrl=" + statusAccessUrl +
                ", accessErrorMessage='" + accessErrorMessage + '\'' +
                ", accessStatusLastChangeDate=" + accessStatusLastChangeDate +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", statusDownloadUrl=" + statusDownloadUrl +
                ", downloadErrorMessage='" + downloadErrorMessage + '\'' +
                ", downloadStatusLastChangeDate=" + downloadStatusLastChangeDate +
                ", licenceId='" + licenceId + '\'' +
                '}';
    }
}
