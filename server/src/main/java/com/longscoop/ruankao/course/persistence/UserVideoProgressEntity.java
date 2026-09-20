package com.longscoop.ruankao.course.persistence;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("user_video_progress")
public class UserVideoProgressEntity {

    private Long userId;
    private Long videoId;
    private Integer progressSeconds;
    private Double completionRate;
    private Boolean completed;
    private Boolean masteryApplied;
    private OffsetDateTime lastWatchAt;
    private OffsetDateTime updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public Integer getProgressSeconds() { return progressSeconds; }
    public void setProgressSeconds(Integer progressSeconds) { this.progressSeconds = progressSeconds; }
    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public Boolean getMasteryApplied() { return masteryApplied; }
    public void setMasteryApplied(Boolean masteryApplied) { this.masteryApplied = masteryApplied; }
    public OffsetDateTime getLastWatchAt() { return lastWatchAt; }
    public void setLastWatchAt(OffsetDateTime lastWatchAt) { this.lastWatchAt = lastWatchAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
