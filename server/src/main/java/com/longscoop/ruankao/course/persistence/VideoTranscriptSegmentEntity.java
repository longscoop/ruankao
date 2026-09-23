package com.longscoop.ruankao.course.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("video_transcript_segment")
public class VideoTranscriptSegmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoId;
    private BigDecimal startSecond;
    private BigDecimal endSecond;
    private String content;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public BigDecimal getStartSecond() { return startSecond; }
    public void setStartSecond(BigDecimal startSecond) { this.startSecond = startSecond; }
    public BigDecimal getEndSecond() { return endSecond; }
    public void setEndSecond(BigDecimal endSecond) { this.endSecond = endSecond; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
