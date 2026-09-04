package com.freelancesuite.dto;

import java.time.LocalDateTime;

public class TimeEntryDto {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long userId;
    private String userName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Boolean isBilled;

    public TimeEntryDto() {}

    public TimeEntryDto(Long id, Long taskId, String taskTitle, Long userId, String userName, LocalDateTime startTime, LocalDateTime endTime, Integer durationMinutes, Boolean isBilled) {
        this.id = id;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.userId = userId;
        this.userName = userName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.isBilled = isBilled;
    }

    public static TimeEntryDtoBuilder builder() { return new TimeEntryDtoBuilder(); }

    public static class TimeEntryDtoBuilder {
        private Long id;
        private Long taskId;
        private String taskTitle;
        private Long userId;
        private String userName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes;
        private Boolean isBilled;

        public TimeEntryDtoBuilder id(Long id) { this.id = id; return this; }
        public TimeEntryDtoBuilder taskId(Long taskId) { this.taskId = taskId; return this; }
        public TimeEntryDtoBuilder taskTitle(String taskTitle) { this.taskTitle = taskTitle; return this; }
        public TimeEntryDtoBuilder userId(Long userId) { this.userId = userId; return this; }
        public TimeEntryDtoBuilder userName(String userName) { this.userName = userName; return this; }
        public TimeEntryDtoBuilder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public TimeEntryDtoBuilder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public TimeEntryDtoBuilder durationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public TimeEntryDtoBuilder isBilled(Boolean isBilled) { this.isBilled = isBilled; return this; }

        public TimeEntryDto build() {
            return new TimeEntryDto(id, taskId, taskTitle, userId, userName, startTime, endTime, durationMinutes, isBilled);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Boolean getIsBilled() { return isBilled; }
    public void setIsBilled(Boolean isBilled) { this.isBilled = isBilled; }
}
