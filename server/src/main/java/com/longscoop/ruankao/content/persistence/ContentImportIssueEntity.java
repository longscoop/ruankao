package com.longscoop.ruankao.content.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.longscoop.ruankao.content.model.ImportIssueSeverity;
import com.longscoop.ruankao.content.model.ImportIssueStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

@TableName("content_import_issue")
public class ContentImportIssueEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private UUID batchId; private Long itemId; private ImportIssueSeverity severity;
    private String code; private String message; private Integer sourcePage;
    private ImportIssueStatus status; private OffsetDateTime createdAt; private OffsetDateTime resolvedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public UUID getBatchId(){return batchId;} public void setBatchId(UUID v){batchId=v;}
    public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    public ImportIssueSeverity getSeverity(){return severity;} public void setSeverity(ImportIssueSeverity v){severity=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public Integer getSourcePage(){return sourcePage;} public void setSourcePage(Integer v){sourcePage=v;}
    public ImportIssueStatus getStatus(){return status;} public void setStatus(ImportIssueStatus v){status=v;}
    public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;}
    public OffsetDateTime getResolvedAt(){return resolvedAt;} public void setResolvedAt(OffsetDateTime v){resolvedAt=v;}
}
