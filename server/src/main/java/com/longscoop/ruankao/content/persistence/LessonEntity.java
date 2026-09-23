package com.longscoop.ruankao.content.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.content.model.LessonStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@TableName("lesson")
public class LessonEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long chapterId;
    private String title;
    private String summary;
    private LessonStatus status;
    private UUID sourceImportBatchId;
    private Integer sourcePageStart;
    private Integer sourcePageEnd;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getChapterId(){return chapterId;} public void setChapterId(Long v){chapterId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
    public LessonStatus getStatus(){return status;} public void setStatus(LessonStatus v){status=v;}
    public UUID getSourceImportBatchId(){return sourceImportBatchId;} public void setSourceImportBatchId(UUID v){sourceImportBatchId=v;}
    public Integer getSourcePageStart(){return sourcePageStart;} public void setSourcePageStart(Integer v){sourcePageStart=v;}
    public Integer getSourcePageEnd(){return sourcePageEnd;} public void setSourcePageEnd(Integer v){sourcePageEnd=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
}
