package com.niniyumi.personalagent.course.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import java.time.Instant;

@TableName("course_audio_parts")
public class CourseAudioPartRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Integer partNumber;
    private Integer durationSeconds;
    private String storagePath;
    private Long fileSize;
    private String transcript;
    private Instant createdAt;

    static CourseAudioPartRow fromDomain(CourseAudioPart part) {
        CourseAudioPartRow row = new CourseAudioPartRow();
        row.id = part.id();
        row.courseId = part.courseId();
        row.partNumber = part.partNumber();
        row.durationSeconds = part.durationSeconds();
        row.storagePath = part.storagePath();
        row.fileSize = part.fileSize();
        row.transcript = part.transcript();
        row.createdAt = part.createdAt();
        return row;
    }

    CourseAudioPart toDomain() {
        return new CourseAudioPart(id, courseId, partNumber, durationSeconds, storagePath, fileSize, transcript, createdAt);
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Integer getPartNumber() { return partNumber; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public String getStoragePath() { return storagePath; }
    public Long getFileSize() { return fileSize; }
    public String getTranscript() { return transcript; }
    public Instant getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public void setPartNumber(Integer partNumber) { this.partNumber = partNumber; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
