package com.freelancesuite.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "platform_state")
public class PlatformState {
    @Id
    private Integer id;
    @Column(nullable = false)
    private boolean bootstrapCompleted;

    public PlatformState() {}
    public PlatformState(Integer id, boolean bootstrapCompleted) {
        this.id = id; this.bootstrapCompleted = bootstrapCompleted;
    }
    public Integer getId() { return id; }
    public boolean isBootstrapCompleted() { return bootstrapCompleted; }
    public void setBootstrapCompleted(boolean bootstrapCompleted) { this.bootstrapCompleted = bootstrapCompleted; }
}
