package com.niniyumi.personalagent.course.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Instant;

@TableName("courses")
public class CourseRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String courseName;
    private java.time.LocalDate lessonDate;
    private CourseStatus status;
    private Integer durationSeconds;
    private Integer processingProgress;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String transcript;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String noteContent;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;
    private String sourceType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String originalAudioPath;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long expectedBytes;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String noteCandidate;
    private Instant createdAt;
    private Instant updatedAt;

    static CourseRow fromDomain(Course course) {
        CourseRow row = new CourseRow();
        row.id = course.id();
        row.userId = course.userId();
        row.title = course.title();
        row.courseName = course.courseName();
        row.lessonDate = course.lessonDate();
        row.status = course.status();
        row.durationSeconds = course.durationSeconds();
        row.processingProgress = course.processingProgress();
        row.transcript = course.transcript();
        row.noteContent = course.noteContent();
        row.errorMessage = course.errorMessage();
        row.sourceType = course.sourceType();
        row.originalAudioPath = course.originalAudioPath();
        row.expectedBytes = course.expectedBytes();
        row.noteCandidate = course.noteCandidate();
        row.createdAt = course.createdAt();
        row.updatedAt = course.updatedAt();
        return row;
    }

    Course toDomain() {
        return new Course(id, userId, title, status, durationSeconds, processingProgress, transcript, noteContent,
                errorMessage, sourceType, originalAudioPath, expectedBytes, noteCandidate, createdAt, updatedAt,
                courseName, lessonDate);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getCourseName() { return courseName; }
    public java.time.LocalDate getLessonDate() { return lessonDate; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setLessonDate(java.time.LocalDate lessonDate) { this.lessonDate = lessonDate; }
    public CourseStatus getStatus() { return status; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getProcessingProgress() { return processingProgress; }
    public String getTranscript() { return transcript; }
    public String getNoteContent() { return noteContent; }
    public String getErrorMessage() { return errorMessage; }
    public String getSourceType() { return sourceType; }
    public String getOriginalAudioPath() { return originalAudioPath; }
    public Long getExpectedBytes() { return expectedBytes; }
    public String getNoteCandidate() { return noteCandidate; }
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
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setOriginalAudioPath(String originalAudioPath) { this.originalAudioPath = originalAudioPath; }
    public void setExpectedBytes(Long expectedBytes) { this.expectedBytes = expectedBytes; }
    public void setNoteCandidate(String noteCandidate) { this.noteCandidate = noteCandidate; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
