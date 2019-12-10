package de.fhg.fokus.piveau.metrics.hub_to_postgres.model;

import java.time.LocalDateTime;

public class Violation {

    private long id;
    private LocalDateTime dbUpdate;
    private String instance;
    private String name;
    private String type;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getDbUpdate() {
        return dbUpdate;
    }

    public void setDbUpdate(LocalDateTime dbUpdate) {
        this.dbUpdate = dbUpdate;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Violation{" +
                "id=" + id +
                ", dbUpdate=" + dbUpdate +
                ", instance='" + instance + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
