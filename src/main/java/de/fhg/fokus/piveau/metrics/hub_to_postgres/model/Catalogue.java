package de.fhg.fokus.piveau.metrics.hub_to_postgres.model;

import java.time.LocalDateTime;

public class Catalogue {

    private long id;
    private String instanceId;
    private LocalDateTime dbUpdate;
    private String name;
    private String title;
    private String description;
    private String spatial;
    private Boolean isDcat;


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

    public LocalDateTime getDbUpdate() {
        return dbUpdate;
    }

    public void setDbUpdate(LocalDateTime dbUpdate) {
        this.dbUpdate = dbUpdate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpatial() {
        return spatial;
    }

    public void setSpatial(String spatial) {
        this.spatial = spatial;
    }

    public Boolean getDcat() {
        return isDcat;
    }

    public void setDcat(Boolean dcat) {
        isDcat = dcat;
    }

    @Override
    public String toString() {
        return "Catalogue{" +
                "id=" + id +
                ", instanceId='" + instanceId + '\'' +
                ", dbUpdate=" + dbUpdate +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", spatial='" + spatial + '\'' +
                ", dcat='" + isDcat + '\'' +
                '}';
    }
}
