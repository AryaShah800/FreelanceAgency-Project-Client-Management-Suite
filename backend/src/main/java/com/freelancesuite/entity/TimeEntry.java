package com.freelancesuite.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_entries")
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationMinutes = 0;

    @Column(nullable = false)
    private Boolean isBilled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    public TimeEntry() {}

    public TimeEntry(Long id, Task task, AppUser user, LocalDateTime startTime, LocalDateTime endTime, Integer durationMinutes, Boolean isBilled, Invoice invoice) {
        this.id = id;
        this.task = task;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes != null ? durationMinutes : 0;
        this.isBilled = isBilled != null ? isBilled : false;
        this.invoice = invoice;
    }

    public static TimeEntryBuilder builder() { return new TimeEntryBuilder(); }

    public static class TimeEntryBuilder {
        private Long id;
        private Task task;
        private AppUser user;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes = 0;
        private Boolean isBilled = false;
        private Invoice invoice;

        public TimeEntryBuilder id(Long id) { this.id = id; return this; }
        public TimeEntryBuilder task(Task task) { this.task = task; return this; }
        public TimeEntryBuilder user(AppUser user) { this.user = user; return this; }
        public TimeEntryBuilder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public TimeEntryBuilder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public TimeEntryBuilder durationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public TimeEntryBuilder isBilled(Boolean isBilled) { this.isBilled = isBilled; return this; }
        public TimeEntryBuilder invoice(Invoice invoice) { this.invoice = invoice; return this; }

        public TimeEntry build() {
            return new TimeEntry(id, task, user, startTime, endTime, durationMinutes, isBilled, invoice);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Boolean getIsBilled() { return isBilled; }
    public void setIsBilled(Boolean isBilled) { this.isBilled = isBilled; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
}
