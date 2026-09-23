package com.longscoop.ruankao.content.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.content.model.ImportBatchStatus;
import com.longscoop.ruankao.content.model.ImportDocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

@TableName("content_import_batch")
public class ContentImportBatchEntity {
    @TableId(type = IdType.INPUT)
    private UUID id;
    private Long examId;
    private String filename;
    private String originalObjectKey;
    private String sha256;
    private ImportDocumentType detectedType;
    private ImportBatchStatus status;
    private String title;
    private Integer pageCount;
    private Long createdBy;
    private Long materializedCourseId;
    private String confirmKey;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public Long getExamId(){return examId;} public void setExamId(Long v){examId=v;}
    public String getFilename(){return filename;} public void setFilename(String v){filename=v;}
    public String getOriginalObjectKey(){return originalObjectKey;} public void setOriginalObjectKey(String v){originalObjectKey=v;}
    public String getSha256(){return sha256;} public void setSha256(String v){sha256=v;}
    public ImportDocumentType getDetectedType(){return detectedType;} public void setDetectedType(ImportDocumentType v){detectedType=v;}
    public ImportBatchStatus getStatus(){return status;} public void setStatus(ImportBatchStatus v){status=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public Integer getPageCount(){return pageCount;} public void setPageCount(Integer v){pageCount=v;}
    public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;}
    public Long getMaterializedCourseId(){return materializedCourseId;} public void setMaterializedCourseId(Long v){materializedCourseId=v;}
    public String getConfirmKey(){return confirmKey;} public void setConfirmKey(String v){confirmKey=v;}
    public OffsetDateTime getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(OffsetDateTime v){confirmedAt=v;}
    public OffsetDateTime getPublishedAt(){return publishedAt;} public void setPublishedAt(OffsetDateTime v){publishedAt=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(OffsetDateTime v){updatedAt=v;}
}
