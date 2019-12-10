package de.fhg.fokus.piveau.metrics.hub_to_postgres.model;

import java.time.LocalDateTime;
import java.util.List;

public class Dataset {

    private long id;
    private String instanceId;
    private LocalDateTime dbUpdate;
    private String licenceId;
    private Boolean isMachineReadable;
    private String name;
    private String title;
    private List<Distribution> distributions;
    private List<Violation> violations;


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

    public String getLicenceId() {
        return licenceId;
    }

    public void setLicenceId(String licenceId) {
        this.licenceId = licenceId;
    }

    public Boolean getMachineReadable() {
        return isMachineReadable;
    }

    public void setMachineReadable(Boolean machineReadable) {
        isMachineReadable = machineReadable;
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

    public List<Distribution> getDistributions() {
        return distributions;
    }

    public void setDistributions(List<Distribution> distributions) {
        this.distributions = distributions;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public void setViolations(List<Violation> violations) {
        this.violations = violations;
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "id=" + id +
                ", instanceId='" + instanceId + '\'' +
                ", dbUpdate=" + dbUpdate +
                ", licenceId='" + licenceId + '\'' +
                ", isMachineReadable=" + isMachineReadable +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", distributions=" + distributions +
                ", violations=" + violations +
                '}';
    }
}
