package com.niniyumi.personalagent.course.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Instant;

@TableName("courses")
public class CourseRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private CourseStatus status;
    private Integer durationSeconds;
    private Integer processingProgress;
    private String transcript;
    private String noteContent;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    static CourseRow fromDomain(Course course) {
        CourseRow row = new CourseRow();
        row.id = course.id();
        row.userId = course.userId();
        row.title = course.title();
        row.status = course.status();
        row.durationSeconds = course.durationSeconds();
        row.processingProgress = course.processingProgress();
        row.transcript = course.transcript();
        row.noteContent = course.noteContent();
        row.errorMessage = course.errorMessage();
        row.createdAt = course.createdAt();
        row.updatedAt = course.updatedAt();
        return row;
    }

    Course toDomain() {
        return new Course(id, userId, title, status, durationSeconds, processingProgress, transcript, noteContent,
                errorMessage, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public CourseStatus getStatus() { return status; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getProcessingProgress() { return processingProgress; }
    public String getTranscript() { return transcript; }
    public String getNoteContent() { return noteContent; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setStatus(CourseStatus status) { this.status = status; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setProcessingProgress(Integer processingProgress) { this.processingProgress = processingProgress; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public void setNoteContent(String noteContent) { this.noteContent = noteContent; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
